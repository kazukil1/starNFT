package cn.kaziki.nft.turbo.collection.domain.service.impl.redis;

import cn.kaziki.nft.turbo.api.collection.request.CollectionPageQueryRequest;
import cn.kaziki.nft.turbo.api.collection.request.CollectionInventoryRequest;
import cn.kaziki.nft.turbo.api.collection.response.CollectionInventoryResponse;
import cn.kaziki.nft.turbo.collection.domain.service.CollectionInventoryService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import static cn.kaziki.nft.turbo.api.collection.constant.CollectionInventoryEnum.DUPLICATED;

@Slf4j
public class CollectionInventoryRedisService implements CollectionInventoryService {
    @Autowired
    private RedissonClient redissonClient;

    private static final String INVENTORY_KEY = "clc:inventory:";
    private static final String INVENTORY_STREAM_KEY = "clc:inventory:stream";

    private static final String ERROR_CODE_INVENTORY_NOT_ENOUGH = "INVENTORY_NOT_ENOUGH";
    private static final String ERROR_CODE_KEY_NOT_FOUND = "KEY_NOT_FOUND";
    private static final String ERROR_CODE_OPERATION_ALREADY_EXECUTED = "OPERATION_ALREADY_EXECUTED";

    /**
     *
     * @param request
     * @return
     */
    @Override
    public CollectionInventoryResponse init(CollectionInventoryRequest request) {
        CollectionInventoryResponse response = new CollectionInventoryResponse();
        if(redissonClient.getBucket(getCacheKey(request)).isExists()){
            response.setSuccess(true);
            response.setResponseCode(DUPLICATED.name());
            return response;
        }
        redissonClient.getBucket(getCacheKey(request)).set(request.getInventory());
        response.setSuccess(true);
        response.setCollectionId(request.getCollectionId());
        response.setIdentifier(request.getIdentifier());
        response.setInventory(request.getInventory());
        return response;
    }

    @Override
    public Integer getInventory(CollectionInventoryRequest request) {
        Integer stock = (Integer) redissonClient.getBucket(getCacheKey(request)).get();
        return stock;
    }

    @Override
    public CollectionInventoryResponse decrease(CollectionInventoryRequest request) {
        CollectionInventoryResponse response = new CollectionInventoryResponse();
        String luaScript = """
                            -- 检查操作是否已经执行过，通过检查哈希表(KEYS[2])中是否存在标识(ARGV[2])
                            if redis.call('hexists', KEYS[2], ARGV[2]) == 1 then
                                -- 如果存在，返回错误信息'OPERATION_ALREADY_EXECUTED'
                                return redis.error_reply('OPERATION_ALREADY_EXECUTED')
                            end
                        
                            -- 获取当前库存值
                            local current = redis.call('get', KEYS[1])
                            -- 如果库存键不存在，返回错误信息'KEY_NOT_FOUND'
                            if current == false then
                                return redis.error_reply('KEY_NOT_FOUND')
                            end
                            -- 如果当前值不是数字，返回错误信息'current value is not a number'
                            if tonumber(current) == nil then
                                return redis.error_reply('current value is not a number')
                            end
                            -- 如果当前库存小于请求的扣减数量，返回错误信息'INVENTORY_NOT_ENOUGH'
                            if tonumber(current) < tonumber(ARGV[1]) then
                                return redis.error_reply('INVENTORY_NOT_ENOUGH')
                            end
                        
                            -- 计算新的库存值
                            local new = tonumber(current) - tonumber(ARGV[1])
                            -- 设置新的库存值
                            redis.call('set', KEYS[1], tostring(new))
                        
                            -- 获取Redis服务器的当前时间（秒和微秒）
                            local time = redis.call("time")
                            -- 转换为毫秒级时间戳
                            local currentTimeMillis = (time[1] * 1000) + math.floor(time[2] / 1000)
                        
                            -- 使用哈希结构存储操作日志
                            redis.call('hset', KEYS[2], ARGV[2], cjson.encode({
                                action = "decrease",  -- 操作类型：减少
                                from = current,       -- 操作前的库存值
                                to = new,             -- 操作后的库存值
                                change = ARGV[1],     -- 变化量
                                by = ARGV[2],         -- 操作标识
                                timestamp = currentTimeMillis  -- 操作时间戳
                            }))
                        
                            -- 返回新的库存值
                            return new
                            """;

        try {
            Integer result = ((Long) redissonClient.getScript().eval(
                    RScript.Mode.READ_WRITE,
                    luaScript,
                    RScript.ReturnType.INTEGER,
                    Arrays.asList(getCacheKey(request),getCacheStreamKey(request)),
                    request.getInventory(),request.getIdentifier()
            )).intValue();
        }catch (RedisException e){
            log.error("decrease error , collectionId = {} , identifier = {} ,",request.getCollectionId(),request.getIdentifier());
            response.setSuccess(false);
            response.setCollectionId(request.getCollectionId());
            response.setIdentifier(request.getIdentifier());
            if(e.getMessage().startsWith(ERROR_CODE_INVENTORY_NOT_ENOUGH)){
                response.setResponseCode(ERROR_CODE_INVENTORY_NOT_ENOUGH);
            } else if(e.getMessage().startsWith(ERROR_CODE_KEY_NOT_FOUND)){
                response.setResponseCode(ERROR_CODE_KEY_NOT_FOUND);
            } else if(e.getMessage().startsWith(ERROR_CODE_OPERATION_ALREADY_EXECUTED)){
                response.setResponseCode(ERROR_CODE_OPERATION_ALREADY_EXECUTED);
            } else {
                response.setResponseCode("业务出错");
            }
            response.setResponseMessage(e.getMessage());
        }
        return response;
    }

    @Override
    public List<Object> getInventoryDecreaseLogs(CollectionInventoryRequest request) {
        return List.of();
    }

    @Override
    public CollectionInventoryResponse increase(CollectionPageQueryRequest request) {
        return null;
    }

    @Override
    public void invaild(CollectionInventoryRequest request) {
        if(redissonClient.getBucket(getCacheKey(request)).isExists()){
            redissonClient.getBucket(getCacheKey(request)).delete();
        }
    }

    @NotNull
    private static String getCacheKey(CollectionInventoryRequest request){
        return INVENTORY_KEY + request.getCollectionId();
    }

    @NotNull
    private static String getCacheStreamKey(CollectionInventoryRequest request){
        return INVENTORY_STREAM_KEY + request.getCollectionId();
    }
}
