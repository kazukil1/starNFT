package cn.kaziki.nft.turbo.order.sharding.id;

import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

/**
 * 雪花算法 WorkerId 持有器
 * <p>
 * 利用 Redis 的 {@code INCR} 原子操作，为分布式部署的每个应用实例分配一个唯一的
 * WorkerId（取值范围 0 ~ 31），避免多实例产生重复的雪花 ID。
 * <p>
 * 核心原理：
 * <ol>
 *     <li>所有实例共享同一个 Redis 自增计数器 {@code clientName}</li>
 *     <li>每个实例启动时调用 {@code incrementAndGet()} 获取一个递增序号</li>
 *     <li>对 32 取模后得到 0~31 范围内的 WorkerId</li>
 * </ol>
 * 注意：
 * <ul>
 *     <li>这是一个简单的分配方案，超过 32 个实例时 WorkerId 会循环复用，
 *         如果已有实例长时间存活可能出现冲突，实际使用需控制实例数量或加校验。</li>
 *     <li>WorkerId 在 JVM 生命周期内保持不变（static 字段），不会因请求重新获取。</li>
 * </ul>
 */
public class WorkerIdHolder implements CommandLineRunner {

    /** Redisson 客户端，用于操作 Redis 原子计数器 */
    private RedissonClient redissonClient;

    /**
     * Redis 自增计数器的 Key 名称
     * <p>
     * 默认值为 "workerId"，可通过配置项 {@code order.client.name} 自定义，
     * 便于多环境隔离（如 order-prod、order-test）。
     */
    @Value("${order.client.name:workerId}")
    private String clientName;

    /**
     * 当前实例分配到的 WorkerId
     * <p>
     * 使用 static 修饰，保证 JVM 全局唯一访问入口，
     * 供雪花算法 ID 生成器直接读取。取值范围：0 ~ 31。
     */
    public static long WORKER_ID;

    /**
     * 构造器注入 RedissonClient
     *
     * @param redissonClient Redisson 客户端实例
     */
    public WorkerIdHolder(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Spring Boot 启动完成回调 —— 触发 WorkerId 分配
     * <p>
     * 在 Spring 容器初始化完成后、Web 服务器对外提供服务前执行，
     * 确保后续雪花算法生成 ID 时 {@link #WORKER_ID} 已经初始化完毕。
     * <p>
     * 执行流程：
     * <ol>
     *     <li>获取 Redis 中名为 {@code clientName} 的原子长整型计数器（不存在则自动创建，初始值 0）</li>
     *     <li>调用 {@code incrementAndGet()} 原子自增并返回新值</li>
     *     <li>对 32 取模得到 0~31 范围内的 WorkerId，赋值给静态字段</li>
     * </ol>
     *
     * @param args 启动参数（由 Spring Boot 透传）
     */
    @Override
    public void run(String... args) throws Exception {
        // RAtomicLong 基于 Redis 的 String 类型实现原子计数器，线程安全且跨 JVM 共享
        RAtomicLong atomicLong = redissonClient.getAtomicLong(clientName);
        // incrementAndGet() = INCR 命令，保证多实例并发启动时每个得到不同序号
        WORKER_ID = atomicLong.incrementAndGet() % 32;
    }
}