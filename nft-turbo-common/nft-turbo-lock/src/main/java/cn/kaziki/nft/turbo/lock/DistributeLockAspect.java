package cn.kaziki.nft.turbo.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面
 */
@Aspect
@Component
@Order(Integer.MIN_VALUE + 1)
public class DistributeLockAspect {

    private RedissonClient redissonClient;

    public DistributeLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private static final Logger LOG = LoggerFactory.getLogger(DistributeLockAspect.class);

    @Around("@annotation(cn.kaziki.nft.turbo.lock.DistributeLock)")
    public Object process(ProceedingJoinPoint pjp) throws Exception {
        // 初始化返回值，默认设为 null，后续在目标方法执行成功后赋值
        Object response = null;

// 通过 AOP 切点签名获取当前被拦截的目标 Method 对象，
// 用于读取方法上的注解信息及参数信息
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

// 从目标方法上提取 @DistributeLock 注解实例，
// 从而拿到 key / keyExpression / expireTime 等锁配置项
        DistributeLock distributeLock = method.getAnnotation(DistributeLock.class);

// 优先读取注解上显式写死的 key 值（静态 key）
        String key = distributeLock.key();

// 判断是否未指定静态 key（即 key 为默认值 NONE_KEY）
        if (DistributeLockConstant.NONE_KEY.equals(key)) {

            // 静态 key 和 SpEL 表达式 keyExpression 同时都没配置 → 无锁键可用，直接抛异常
            if (DistributeLockConstant.NONE_KEY.equals(distributeLock.keyExpression())) {
                throw new DistributeLockException("no lock key found...");
            }

            // 使用 Spring Expression Language (SpEL) 解析动态 key 表达式
            SpelExpressionParser parser = new SpelExpressionParser();
            Expression expression = parser.parseExpression(distributeLock.keyExpression());

            // 创建 SpEL 运行时上下文，用于向表达式注入方法参数变量，
            // 这样表达式中就可以通过 #paramName 引用方法入参
            EvaluationContext context = new StandardEvaluationContext();
            // 获取参数值
            Object[] args = pjp.getArgs();

            // 获取运行时参数的名称
            StandardReflectionParameterNameDiscoverer discoverer
                    = new StandardReflectionParameterNameDiscoverer();
            String[] parameterNames = discoverer.getParameterNames(method);

            // 将参数绑定到context中
            if (parameterNames != null) {
                for (int i = 0; i < parameterNames.length; i++) {
                    context.setVariable(parameterNames[i], args[i]);
                }
            }

            // 解析表达式，获取结果
            key = String.valueOf(expression.getValue(context));
        }

        String scene = distributeLock.scene();

        String lockKey = scene + "#" + key;

        // 获取锁的过期时间和等待时间配置
        int expireTime = distributeLock.expireTime();
        int waitTime = distributeLock.waitTime();
        // 根据 lockKey 从 Redisson 获取分布式锁实例
        RLock rLock = redissonClient.getLock(lockKey);
        boolean lockResult = false;

        // ========== 加锁策略选择 ==========
        // 根据 waitTime 和 expireTime 是否为默认值，组合出四种加锁场景：
        //   1. 两者均为默认值 -> 阻塞锁 + 看门狗续期（推荐，最常用）
        //   2. waitTime 默认，expireTime 指定 -> 阻塞锁 + 固定过期时间
        //   3. waitTime 指定，expireTime 默认 -> 限时尝试锁 + 看门狗续期
        //   4. 两者均指定 -> 限时尝试锁 + 固定过期时间
        if (waitTime == DistributeLockConstant.DEFAULT_WAIT_TIME) {
            // waitTime 为默认值（-1），表示一直阻塞等待直到获取锁
            if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {
                // 场景1：不传过期时间，Redisson 会启动 30s 看门狗自动续期
                LOG.info(String.format("lock for key : %s", lockKey));
                rLock.lock();
            } else {
                // 场景2：指定过期时间，锁到期后自动释放（不会自动续期）
                LOG.info(String.format("lock for key : %s , expire : %s", lockKey, expireTime));
                rLock.lock(expireTime, TimeUnit.MILLISECONDS);
            }
            // 阻塞锁成功获取后直接标记为 true
            lockResult = true;
        } else {
            // waitTime 有具体值，表示只在指定时间内尝试获取锁，超时则放弃
            if (expireTime == DistributeLockConstant.DEFAULT_EXPIRE_TIME) {
                // 场景3：在 waitTime 内尝试获取锁，获取到后启动看门狗续期
                LOG.info(String.format("try lock for key : %s , wait : %s", lockKey, waitTime));
                lockResult = rLock.tryLock(waitTime, TimeUnit.MILLISECONDS);
            } else {
                // 场景4：在 waitTime 内尝试获取锁，锁的持有时间为 expireTime（不会自动续期）
                LOG.info(String.format("try lock for key : %s , expire : %s , wait : %s", lockKey, expireTime, waitTime));
                lockResult = rLock.tryLock(waitTime, expireTime, TimeUnit.MILLISECONDS);
            }
        }

        // 加锁失败：记录告警日志并抛出业务异常，避免后续并发执行
        if (!lockResult) {
            LOG.warn(String.format("lock failed for key : %s , expire : %s", lockKey, expireTime));
            throw new DistributeLockException("acquire lock failed... key : " + lockKey);
        }

        try {
            // 加锁成功，执行被拦截的目标方法
            LOG.info(String.format("lock success for key : %s , expire : %s", lockKey, expireTime));
            response = pjp.proceed();
        } catch (Throwable e) {
            // 将目标方法抛出的异常包装为 Exception 向上传播
            throw new Exception(e);
        } finally {
            // 无论执行成功还是异常，都必须释放锁，防止死锁
            rLock.unlock();
            LOG.info(String.format("unlock for key : %s , expire : %s", lockKey, expireTime));
        }
        return response;
    }
}