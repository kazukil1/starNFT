package cn.kaziki.nft.turbo.inventory.domain.service.impl;

import cn.kaziki.nft.turbo.api.inventory.request.InventoryRequest;
import cn.kaziki.nft.turbo.inventory.domain.response.InventoryResponse;
import cn.kaziki.nft.turbo.inventory.domain.service.InventoryService;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.redisson.client.codec.LongCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.kaziki.nft.turbo.base.response.ResponseCode.BIZ_ERROR;
import static cn.kaziki.nft.turbo.base.response.ResponseCode.DUPLICATED;

/**
 * 库存服务通用实现-基于Redis
 */
public abstract class AbstractInventoryRedisService implements InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(AbstractInventoryRedisService.class);

    @Autowired
    private RedissonClient redissonClient;

    public static final String ERROR_CODE_INVENTORY_NOT_ENOUGH = "INVENTORY_NOT_ENOUGH";
    public static final String ERROR_CODE_INVENTORY_IS_ZERO = "INVENTORY_IS_ZERO";
    public static final String ERROR_CODE_KEY_NOT_FOUND = "KEY_NOT_FOUND";
    public static final String ERROR_CODE_OPERATION_ALREADY_EXECUTED = "OPERATION_ALREADY_EXECUTED";

    // 初始化商品缓存
    @Override
    public InventoryResponse init(InventoryRequest request) {
        InventoryResponse inventoryResponse = new InventoryResponse();
        // 查重
        if (redissonClient.getBucket(getCacheKey(request)).isExists()) {
            inventoryResponse.setSuccess(true);
            inventoryResponse.setResponseCode(DUPLICATED.name());
            return inventoryResponse;
        }
        // 新建缓存
        redissonClient.getBucket(getCacheKey(request), LongCodec.INSTANCE).set(request.getInventory());
        inventoryResponse.setSuccess(true);
        inventoryResponse.setGoodsId(request.getGoodsId());
        inventoryResponse.setGoodsType(request.getGoodsType());
        inventoryResponse.setIdentifier(request.getIdentifier());
        inventoryResponse.setInventory(request.getInventory());
        return inventoryResponse;
    }

    @Override
    public Integer getInventory(InventoryRequest request) {
        Long stock = (Long) redissonClient.getBucket(getCacheKey(request), LongCodec.INSTANCE).get();
        return stock != null ? stock.intValue() : null;
    }

    @Override
    public InventoryResponse decrease(InventoryRequest request) {
        InventoryResponse inventoryResponse = new InventoryResponse();
        //幂等校验:判断是否有库存扣减流水:key为goodsId+identifier,value为change
        String luaScript = """
                -- 已有库存扣减流水
                if redis.call('hexists', KEYS[2], ARGV[2]) == 1 then
                    return redis.error_reply('OPERATION_ALREADY_EXECUTED')
                end
                // 查询当前库存
                local current = redis.call('get', KEYS[1])
                if current == false then
                    return redis.error_reply('KEY_NOT_FOUND')
                end
                if tonumber(current) == nil then
                    return redis.error_reply('current value is not a number')
                end
                -- 库存为0
                if tonumber(current) == 0 then
                    return redis.error_reply('INVENTORY_IS_ZERO')
                end
                -- 库存不足
                if tonumber(current) < tonumber(ARGV[1]) then
                    return redis.error_reply('INVENTORY_NOT_ENOUGH')
                end
                -- new为新的剩余库存     
                local new = tonumber(current) - tonumber(ARGV[1])
                redis.call('set', KEYS[1], tostring(new))
                                
                -- 获取Redis服务器的当前时间（秒和微秒）
                local time = redis.call("time")
                -- 转换为毫秒级时间戳
                local currentTimeMillis = (time[1] * 1000) + math.floor(time[2] / 1000)
                                
                -- 使用哈希结构存储日志
                redis.call('hset', KEYS[2], ARGV[2], cjson.encode({
                    action = "decrease",
                    from = current,
                    to = new,
                    change = ARGV[1],
                    by = ARGV[2],
                    timestamp = currentTimeMillis
                }))
                                
                return new
                """;

        try {
            // 1.修改缓存库存
            Long result = (redissonClient.getScript().eval(RScript.Mode.READ_WRITE,//1.读写模式
                    luaScript,//2.lua脚本
                    RScript.ReturnType.INTEGER,//3.返回类型
                    Arrays.asList(getCacheKey(request),//keys[1]=库存key
                            getCacheStreamKey(request)),//keys[2]=库存流水key
                    request.getInventory().intValue(), //argv[1]=扣减数量
                    "DECREASE_" + request.getIdentifier()));//args[2]=

            inventoryResponse.setSuccess(true);
            inventoryResponse.setGoodsId(request.getGoodsId());
            inventoryResponse.setGoodsType(request.getGoodsType());
            inventoryResponse.setIdentifier(request.getIdentifier());
            inventoryResponse.setInventory(result);
            return inventoryResponse;

        } catch (RedisException e) {
            logger.error("decrease error , goodsId = {} , identifier = {} ,", request.getGoodsId(), request.getIdentifier(), e);
            inventoryResponse.setSuccess(false);
            inventoryResponse.setGoodsId(request.getGoodsId());
            inventoryResponse.setGoodsType(request.getGoodsType());
            inventoryResponse.setIdentifier(request.getIdentifier());
            if (e.getMessage().startsWith(ERROR_CODE_INVENTORY_NOT_ENOUGH)) {
                inventoryResponse.setResponseCode(ERROR_CODE_INVENTORY_NOT_ENOUGH);
            } else if (e.getMessage().startsWith(ERROR_CODE_INVENTORY_IS_ZERO)) {
                inventoryResponse.setResponseCode(ERROR_CODE_INVENTORY_IS_ZERO);
            } else if (e.getMessage().startsWith(ERROR_CODE_KEY_NOT_FOUND)) {
                inventoryResponse.setResponseCode(ERROR_CODE_KEY_NOT_FOUND);
            } else if (e.getMessage().startsWith(ERROR_CODE_OPERATION_ALREADY_EXECUTED)) {
                inventoryResponse.setResponseCode(ERROR_CODE_OPERATION_ALREADY_EXECUTED);
                inventoryResponse.setSuccess(true);
            } else {
                inventoryResponse.setResponseCode(BIZ_ERROR.name());
            }
            inventoryResponse.setResponseMessage(e.getMessage());

            return inventoryResponse;
        }
    }

    // 查询库存扣减流水
    @Override
    public String getInventoryDecreaseLog(InventoryRequest request) {
        String luaScript = """
                local jsonString = redis.call('hget', KEYS[1], ARGV[1])
                return jsonString
                """;

        String stream = redissonClient.getScript().eval(RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.STATUS,
                Arrays.asList(getCacheStreamKey(request)), "DECREASE_" + request.getIdentifier());
        return stream;
    }

    @Override
    public String getInventoryIncreaseLog(InventoryRequest request) {
        String luaScript = """
                local jsonString = redis.call('hget', KEYS[1], ARGV[1])
                return jsonString
                """;

        String stream = redissonClient.getScript().eval(RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.STATUS,
                Arrays.asList(getCacheStreamKey(request)), "INCREASE_" + request.getIdentifier());
        return stream;
    }

    @Override
    public List<String> getInventoryDecreaseLogs(InventoryRequest request) {
        String luaScript = """
                local jsonString = redis.call('hvals', KEYS[1])
                return jsonString
                """;

        List<String> stream = redissonClient.getScript().eval(RScript.Mode.READ_ONLY,
                luaScript,
                RScript.ReturnType.STATUS,
                Arrays.asList(getCacheStreamKey(request)),
                Collections.emptyList());
        return stream;
    }

    @Override
    public Long removeInventoryDecreaseLog(InventoryRequest request) {
        String luaScript = """
                local jsonString = redis.call('hdel', KEYS[1], ARGV[1])
                return jsonString
                """;

        Long stream = redissonClient.getScript().eval(RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Arrays.asList(getCacheStreamKey(request)), "DECREASE_" + request.getIdentifier());
        return stream;
    }

    @Override
    public Long removeInventoryIncreaseLog(InventoryRequest request) {
        String luaScript = """
                local jsonString = redis.call('hdel', KEYS[1], ARGV[1])
                return jsonString
                """;

        Long stream = redissonClient.getScript().eval(RScript.Mode.READ_WRITE,
                luaScript,
                RScript.ReturnType.INTEGER,
                Arrays.asList(getCacheStreamKey(request)), "INCREASE_" + request.getIdentifier());
        return stream;
    }

    /**
     * 原子性增加库存（补货/回滚）
     * <p>
     * 与 {@link #decrease} 逻辑对称，通过 Lua 脚本保证增加操作的原子性：
     * <ol>
     *     <li>幂等校验：检查增加流水是否已存在</li>
     *     <li>库存 Key 存在性校验</li>
     *     <li>计算新库存并写回 Redis</li>
     *     <li>记录增加流水，用于对账和幂等</li>
     * </ol>
     *
     * @param request 增加请求，identifier 用于幂等去重
     * @return 增加结果，包含新的库存值或错误码
     */
    @Override
    public InventoryResponse increase(InventoryRequest request) {
        InventoryResponse inventoryResponse = new InventoryResponse();
        // ====================================================================
        // Lua 脚本参数约定：
        //   KEYS[1] = 库存存储 Key
        //   KEYS[2] = 库存流水 Hash Key
        //   ARGV[1] = 本次增加数量
        //   ARGV[2] = 操作唯一标识（格式：INCREASE_<identifier>），用于幂等校验
        // ====================================================================
        String luaScript = """
                if redis.call('hexists', KEYS[2], ARGV[2]) == 1 then
                    return redis.error_reply('OPERATION_ALREADY_EXECUTED')
                end
                                
                local current = redis.call('get', KEYS[1])
                if current == false then
                    return redis.error_reply('key not found')
                end
                if tonumber(current) == nil then
                    return redis.error_reply('current value is not a number')
                end
                                
                local new = tonumber(current) + tonumber(ARGV[1])
                redis.call('set', KEYS[1], tostring(new))
                                
                -- 获取Redis服务器的当前时间（秒和微秒）
                local time = redis.call("time")
                -- 转换为毫秒级时间戳
                local currentTimeMillis = (time[1] * 1000) + math.floor(time[2] / 1000)
                                
                -- 使用哈希结构存储日志
                redis.call('hset', KEYS[2], ARGV[2], cjson.encode({
                    action = "increase",
                    from = current,
                    to = new,
                    change = ARGV[1],
                    by = ARGV[2],
                    timestamp = currentTimeMillis
                }))
                                
                return new
                """;

        try {
            Long result = (redissonClient.getScript().eval(RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.INTEGER,
                    Arrays.asList(getCacheKey(request), getCacheStreamKey(request)),
                    request.getInventory().intValue(), "INCREASE_" + request.getIdentifier()));

            inventoryResponse.setSuccess(true);
            inventoryResponse.setGoodsId(request.getGoodsId());
            inventoryResponse.setGoodsType(request.getGoodsType());
            inventoryResponse.setIdentifier(request.getIdentifier());
            inventoryResponse.setInventory(result);
            return inventoryResponse;

        } catch (RedisException e) {
            logger.error("increase error , goodsId = {} , identifier = {} ,", request.getGoodsId(), request.getIdentifier(), e);
            inventoryResponse.setSuccess(false);
            inventoryResponse.setGoodsId(request.getGoodsId());
            inventoryResponse.setGoodsType(request.getGoodsType());
            inventoryResponse.setIdentifier(request.getIdentifier());
            // 增加操作只涉及 Key 不存在和幂等命中两种常见异常场景
            if (e.getMessage().startsWith(ERROR_CODE_KEY_NOT_FOUND)) {
                inventoryResponse.setResponseCode(ERROR_CODE_KEY_NOT_FOUND);
            } else if (e.getMessage().startsWith(ERROR_CODE_OPERATION_ALREADY_EXECUTED)) {
                // 幂等命中：已执行过的增加操作视为成功返回
                inventoryResponse.setResponseCode(ERROR_CODE_OPERATION_ALREADY_EXECUTED);
                inventoryResponse.setSuccess(true);
            } else {
                inventoryResponse.setResponseCode(BIZ_ERROR.name());
            }
            inventoryResponse.setResponseMessage(e.getMessage());

            return inventoryResponse;
        }
    }

    /**
     * 使库存缓存失效
     * <p>
     * 库存 Key 会被立即删除，而流水记录不会立即删除，
     * 而是设置为 24 小时后过期，这样做的原因是：
     * 在库存缓存失效后仍可通过流水记录进行对账，避免数据丢失导致对账异常。
     *
     * @param request 失效请求
     */
    @Override
    public void invalid(InventoryRequest request) {
        // 立即删除库存 Key
        if (redissonClient.getBucket(getCacheKey(request)).isExists()) {
            redissonClient.getBucket(getCacheKey(request)).delete();
        }

        // 流水记录延迟 24 小时过期，保证对账期间流水可查
        if (redissonClient.getBucket(getCacheStreamKey(request)).isExists()) {
            redissonClient.getBucket(getCacheStreamKey(request)).expire(Instant.now().plus(24, ChronoUnit.HOURS));
        }
    }

    /**
     * 获取库存缓存的key
     * @param request
     * @return
     */
    protected abstract String getCacheKey(InventoryRequest request);

    /**
     * 获取库存流水缓存的key
     * @param request
     * @return
     */
    protected abstract String getCacheStreamKey(InventoryRequest request);
}