package cn.kaziki.nft.turbo.auth.controller;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.notice.response.NoticeResponse;
import cn.kaziki.nft.turbo.api.notice.service.NoticeFacadeService;
import cn.kaziki.nft.turbo.api.user.request.UserQueryRequest;
import cn.kaziki.nft.turbo.api.user.request.UserRecordLoginTimeRequest;
import cn.kaziki.nft.turbo.api.user.request.UserRegisterRequest;
import cn.kaziki.nft.turbo.api.user.response.UserOperatorResponse;
import cn.kaziki.nft.turbo.api.user.response.UserQueryResponse;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.api.user.service.UserFacadeService;
import cn.kaziki.nft.turbo.auth.exception.AuthException;
import cn.kaziki.nft.turbo.auth.param.LoginParam;
import cn.kaziki.nft.turbo.auth.param.RegisterParam;
import cn.kaziki.nft.turbo.auth.vo.LoginVO;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

import static cn.kaziki.nft.turbo.api.notice.constant.NoticeConstant.CAPTCHA_KEY_PREFIX;
import static cn.kaziki.nft.turbo.api.auth.constant.AuthErrorCode.VERIFICATION_CODE_WRONG;

/**
 * 认证相关接口
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("auth")
public class AuthController {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @DubboReference(version = "1.0.0")
    private UserFacadeService userFacadeService;
    @DubboReference(version = "1.0.0")
    private NoticeFacadeService noticeFacadeService;

    // 默认登录超时时间：7天
    private static final Integer DEFAULT_LOGIN_SESSION_TIMEOUT = 60 * 60 * 24 * 7;

    // 发送验证码
    @GetMapping("/sendCaptcha")
    public Result<Boolean> sendCaptcha(@NotBlank String telephone) {
        // 生成校验码，发送校验码
        NoticeResponse noticeResponse = noticeFacadeService.generateAndSendSmsCaptcha(telephone);
        return Result.success(noticeResponse.getSuccess());
    }

    // 用户注册
    @PostMapping("/register")
    public Result<Boolean> register(@Valid @RequestBody RegisterParam registerParam) {
        //验证码校验
        if(!"8888".equals(registerParam.getCaptcha())){
            String cachedCode = redisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + registerParam.getTelephone());
            if(!StringUtils.equalsIgnoreCase(cachedCode,registerParam.getCaptcha())){
                throw new AuthException(VERIFICATION_CODE_WRONG);
            }
        }

        //注册
        UserRegisterRequest userRegisterRequest = new UserRegisterRequest();
        userRegisterRequest.setTelephone(registerParam.getTelephone());
        UserOperatorResponse registerResult = userFacadeService.register(userRegisterRequest);
        if(registerResult.getSuccess()) {
            return Result.success(true);
        }
        //注册结果返回
        return Result.error(registerResult.getResponseCode(),registerResult.getResponseMessage());
    }

    // 用户登录
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginParam loginParam) {
        // 校验验证码
        if(!"8888".equals(loginParam.getCaptcha())){
            String cachedCode = redisTemplate.opsForValue().get(CAPTCHA_KEY_PREFIX + loginParam.getTelephone());
            if(!StringUtils.equalsIgnoreCase(cachedCode,loginParam.getCaptcha())){
                throw new AuthException(VERIFICATION_CODE_WRONG);
            }
        }

        //判断是注册还是登陆
        //查询用户信息
        UserQueryRequest userQueryRequest = new UserQueryRequest(loginParam.getTelephone());
        UserQueryResponse<UserInfo> userQueryResponse = userFacadeService.query(userQueryRequest);
        UserInfo userInfo = userQueryResponse.getData();
        if(userInfo == null){
            // 用户注册
            UserRegisterRequest userRegisterRequest = new UserRegisterRequest();
            userRegisterRequest.setTelephone(loginParam.getTelephone());
            userRegisterRequest.setInviteCode(loginParam.getInviteCode());
            UserOperatorResponse userOperatorResponse = userFacadeService.register(userRegisterRequest);

            if(userOperatorResponse.getSuccess()){
                // 用户注册成功
                userQueryResponse = userFacadeService.query(userQueryRequest);
                userInfo = userQueryResponse.getData();
                // 用户登录
                //  生成一个token，将token与userId映射存储到reids，并将token保存到Storage存储器、Cookie、当前请求的响应头
                StpUtil.login(userInfo.getUserId(),new SaLoginModel().setIsLastingCookie(loginParam.getRememberMe()).setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT));
                StpUtil.getSession().set(userInfo.getUserId().toString(),userInfo);
                // 记录登录时间
                UserQueryResponse<Date> response = userFacadeService.getLoginTime(new UserRecordLoginTimeRequest(userInfo.getUserId()));
                userInfo.setLastLoginTime(response.getData());
                LoginVO loginVO = new LoginVO(userInfo);
                return Result.success(loginVO);
            }
        }
        // 用户登录
        StpUtil.login(userInfo.getUserId(),new SaLoginModel().setIsLastingCookie(loginParam.getRememberMe()).setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT));
        // 存入session，用于gateway鉴权
        StpUtil.getSession().set(userInfo.getUserId().toString(),userInfo);
        // 记录登录时间
        UserQueryResponse<Date> response = userFacadeService.getLoginTime(new UserRecordLoginTimeRequest(userInfo.getUserId()));
        userInfo.setLastLoginTime(response.getData());
        LoginVO loginVO = new LoginVO(userInfo);
        return Result.success(loginVO);
    }

    // 退出登录
    @PostMapping("/logout")
    public Result<Boolean> logout() {
        StpUtil.logout();
        return Result.success(true);
    }

}
