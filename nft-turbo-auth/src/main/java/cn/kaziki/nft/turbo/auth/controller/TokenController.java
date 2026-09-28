package cn.kaziki.nft.turbo.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.auth.constant.AuthErrorCode;
import cn.kaziki.nft.turbo.api.goods.constant.GoodsType;
import cn.kaziki.nft.turbo.api.goods.model.BaseGoodsVO;
import cn.kaziki.nft.turbo.api.goods.service.GoodsFacadeService;
import cn.kaziki.nft.turbo.auth.constant.TokenSceneEnum;
import cn.kaziki.nft.turbo.auth.exception.AuthException;
import cn.kaziki.nft.turbo.web.util.TokenUtil;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static cn.kaziki.nft.turbo.cache.constant.CacheConstant.CACHE_KEY_SEPARATOR;
import static cn.kaziki.nft.turbo.web.util.TokenUtil.TOKEN_PREFIX;

/**
 *  token
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("token")
public class TokenController {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @DubboReference(version = "1.0.0")
    private GoodsFacadeService goodsFacadeService;

    // 获取token
    // 用户进入下单页面，会生成校验token，每次下单校验并删除这个token
    @GetMapping("/get")
    public Result<String> get(@NotBlank String scene, @NotBlank String key) {
        /**
         * 检查下key是不是合法的值（存在的商品id），如果不合法，拒绝生成token，避免攻击者传入一堆随机的key来生成token。
         * 如果做的再好点，商品id不用自增id，而是雪花算法等方式生成，避免攻击者穷举
         *
         * 但是在后面校验token的时候，还是有个问题，那就是我们其实没有校验token对应的商品和下单的商品是不是同一个。这块大家可以自行实现一下。
         */
        //buy:key:id
        TokenSceneEnum tokenScene = Arrays.stream(TokenSceneEnum.values()).filter(tokenSceneEnum -> tokenSceneEnum.getScene().equals(scene))
                .findFirst()
                .orElseThrow(() -> new AuthException(AuthErrorCode.TOKEN_SCENE_NOT_EXIST));

        BaseGoodsVO baseGoodsVO = goodsFacadeService.getGoods(key, getGoodsType(tokenScene));

        if (baseGoodsVO == null || !baseGoodsVO.available()) {
            throw new AuthException(AuthErrorCode.TOKEN_KEY_IS_ILLEGAL);
        }

        if (StpUtil.isLogin()) {
            String userId = (String) StpUtil.getLoginId();
            //key：buy:29:10085：这样设计，每个用户的每个商品的key都会不一样
            String tokenKey = TOKEN_PREFIX + scene + CACHE_KEY_SEPARATOR + userId + CACHE_KEY_SEPARATOR + key;
            //buy:29:10085_YZdkYfQ8fy7biSTsS5oZrbsB8eN7dHPgtCV0dw/36AHSfDQzWOj+ULNEcMluHvep/txjP+BqVRH3JlprS8tWrQ==
            String tokenValue = TokenUtil.getTokenValueByKey(tokenKey);
            stringRedisTemplate.opsForValue().set(tokenKey, tokenValue, 30, TimeUnit.MINUTES);
            return Result.success(tokenValue);
        }
        throw new AuthException(AuthErrorCode.USER_NOT_LOGIN);
    }

    private GoodsType getGoodsType(TokenSceneEnum tokenScene) {

        return switch (tokenScene) {
            case BUY_COLLECTION -> GoodsType.COLLECTION;
            case BUY_BLIND_BOX -> GoodsType.BLIND_BOX;
            case BUY_STAR -> GoodsType.STAR;
            default -> throw new AuthException(AuthErrorCode.TOKEN_SCENE_NOT_EXIST);
        };
    }
}
