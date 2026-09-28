package cn.kaziki.nft.turbo.user.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.kaziki.nft.turbo.api.chain.constant.ChainOperateBizTypeEnum;
import cn.kaziki.nft.turbo.api.chain.request.ChainProcessRequest;
import cn.kaziki.nft.turbo.api.chain.response.ChainProcessResponse;
import cn.kaziki.nft.turbo.api.chain.response.data.ChainCreateData;
import cn.kaziki.nft.turbo.api.chain.service.ChainFacadeService;
import cn.kaziki.nft.turbo.api.user.request.UserActiveRequest;
import cn.kaziki.nft.turbo.api.user.request.UserAuthRequest;
import cn.kaziki.nft.turbo.api.user.request.UserModifyRequest;
import cn.kaziki.nft.turbo.api.user.response.UserOperatorResponse;
import cn.kaziki.nft.turbo.api.user.response.data.BasicUserInfo;
import cn.kaziki.nft.turbo.api.user.response.data.UserInfo;
import cn.kaziki.nft.turbo.file.FileService;
import cn.kaziki.nft.turbo.user.domain.entity.User;
import cn.kaziki.nft.turbo.user.domain.entity.convertor.UserConvertor;
import cn.kaziki.nft.turbo.user.domain.service.UserService;
import cn.kaziki.nft.turbo.user.infrastructure.exception.UserException;
import cn.kaziki.nft.turbo.user.param.UserAuthParam;
import cn.kaziki.nft.turbo.user.param.UserModifyParam;
import cn.kaziki.nft.turbo.web.vo.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.APP_NAME_UPPER;
import static cn.kaziki.nft.turbo.api.common.constant.CommonConstant.SEPARATOR;
import static cn.kaziki.nft.turbo.user.infrastructure.exception.UserErrorCode.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private ChainFacadeService chainFacadeService;
    @Autowired
    private FileService fileService;

    // 获取用户信息
    @GetMapping("/getUserInfo")
    public Result<UserInfo> getUserInfo() {
        String userId = (String) StpUtil.getLoginId();
        User user = userService.findById(Long.valueOf(userId));

        if (user == null) {
            throw new UserException(USER_NOT_EXIST);
        }
        return Result.success(UserConvertor.INSTANCE.mapToVo(user));
    }

    // 根据电话号码获取用户信息
    @GetMapping("/queryUserByTel")
    public Result<BasicUserInfo> queryUserByTel(String telephone) {
        String userId = (String) StpUtil.getLoginId();
        User user = userService.findByTelephone(telephone);

        if (user == null) {
            throw new UserException(USER_NOT_EXIST);
        }
        return Result.success(UserConvertor.INSTANCE.mapToBasicVo(user));
    }

    // 修改昵称
    @PostMapping("/modifyNickName")
    public Result<Boolean> modifyNickName(@Valid @RequestBody UserModifyParam userModifyParam) {
        //从session中查询用户ID
        String loginId = (String) StpUtil.getLoginId();

        //修改信息
        UserModifyRequest request = new UserModifyRequest();
        request.setUserId(Long.valueOf(loginId));
        request.setNickName(userModifyParam.getNickName());
        UserOperatorResponse response = userService.modify(request);

        return Result.success(Boolean.TRUE);
    }

    @PostMapping("/modifyPassword")
    public Result<Boolean> modifyPassword(@Valid @RequestBody UserModifyParam userModifyParam) {
        //从session中查询用户ID

        //修改信息
        return Result.success(Boolean.TRUE);
    }

    // 修改头像
    @PostMapping("/modifyProfilePhoto")
    public Result<String> modifyProfilePhoto(@RequestParam("file_data") MultipartFile file) throws Exception {
        //从session中查询用户ID
        String userId = (String) StpUtil.getLoginId();
        String prefix = "https://nfturbo-yueyu.oss-cn-beijing.aliyuncs.com/";

        if (null == file) {
            throw new UserException(USER_UPLOAD_PICTURE_FAIL);
        }
        String filename = file.getOriginalFilename();
        InputStream fileStream = file.getInputStream();
        String path = "profile/" + userId + "/" + filename;
        boolean res = fileService.upload(path, fileStream);
        if (!res) {
            throw new UserException(USER_UPLOAD_PICTURE_FAIL);
        }

        //修改信息
        UserModifyRequest request = new UserModifyRequest();
        request.setUserId(Long.valueOf(userId));
        request.setProfilePhotoUrl(prefix + path);
        res = userService.modify(request).getSuccess();
        if (!res) {
            throw new UserException(USER_UPLOAD_PICTURE_FAIL);
        }
        return Result.success(prefix + path);
    }

    // 实名认证
    @PostMapping("/auth")
    public Result<Boolean> auth(@Valid @RequestBody UserAuthParam param) {
        String userId = (String) StpUtil.getLoginId();
        // 实名认证
        UserAuthRequest userAuthRequest = new UserAuthRequest();
        userAuthRequest.setUserId(Long.valueOf(userId));
        userAuthRequest.setRealName(param.getRealName());
        userAuthRequest.setIdCard(param.getIdCard());
        UserOperatorResponse authResponse = userService.auth(userAuthRequest);

        if (!authResponse.getSuccess()) {
            return Result.error(authResponse.getResponseCode(), authResponse.getResponseMessage());
        }

        // 认证成功，进行上链
        ChainProcessRequest chainProcessRequest = new ChainProcessRequest();
        chainProcessRequest.setUserId(userId);
        // NFTURBO_CUSTOMER_29
        String identifier = APP_NAME_UPPER + SEPARATOR + authResponse.getUser().getUserRole() + SEPARATOR + userId;
        chainProcessRequest.setIdentifier(identifier);
        chainProcessRequest.setBizId(userService.findById(Long.valueOf(userId)).getTelephone());
        chainProcessRequest.setBizType(ChainOperateBizTypeEnum.USER.name());
        ChainProcessResponse<ChainCreateData> chainProcessResponse = chainFacadeService.createAddr(chainProcessRequest);
        if (chainProcessResponse.getSuccess()) {
            // 激活账户
            ChainCreateData chainCreateData = chainProcessResponse.getData();
            UserActiveRequest userActiveRequest = new UserActiveRequest();
            userActiveRequest.setUserId(userId);
            userActiveRequest.setBlockChainPlatform(chainCreateData.getPlatform());
            userActiveRequest.setBlockChainUrl(chainCreateData.getAccount());
            UserOperatorResponse activeResponse = userService.active(userActiveRequest);
            if (activeResponse.getSuccess()) {
                refreshUserInSession(userId);
                return Result.success(true);
            }
            return Result.error(activeResponse.getResponseCode(), activeResponse.getResponseMessage());
        }
        return Result.error(chainProcessResponse.getResponseCode(), chainProcessResponse.getResponseMessage());
    }

    // 更新当前会话
    private void refreshUserInSession(String userId) {
        User user = userService.getById(userId);
        UserInfo userInfo = UserConvertor.INSTANCE.mapToVo(user);
        StpUtil.getSession().set(userInfo.getUserId().toString(), userInfo);
    }
}
