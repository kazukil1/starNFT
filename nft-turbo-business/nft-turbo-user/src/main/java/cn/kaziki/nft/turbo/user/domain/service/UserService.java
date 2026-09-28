package cn.kaziki.nft.turbo.user.domain.service;

import cn.kaziki.nft.turbo.api.user.constant.UserOperateTypeEnum;
import cn.kaziki.nft.turbo.api.user.constant.UserStateEnum;
import cn.kaziki.nft.turbo.api.user.request.UserActiveRequest;
import cn.kaziki.nft.turbo.api.user.request.UserAuthRequest;
import cn.kaziki.nft.turbo.api.user.request.UserModifyRequest;
import cn.kaziki.nft.turbo.api.user.response.UserOperatorResponse;
import cn.kaziki.nft.turbo.api.user.response.data.InviteRankInfo;
import cn.kaziki.nft.turbo.base.exception.BizException;
import cn.kaziki.nft.turbo.base.exception.RepoErrorCode;
import cn.kaziki.nft.turbo.base.response.PageResponse;
import cn.kaziki.nft.turbo.lock.DistributeLock;
import cn.kaziki.nft.turbo.user.domain.entity.User;
import cn.kaziki.nft.turbo.user.domain.entity.convertor.UserConvertor;
import cn.kaziki.nft.turbo.user.infrastructure.exception.UserException;
import cn.kaziki.nft.turbo.user.infrastructure.mapper.UserMapper;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheRefresh;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.template.QuickConfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.kaziki.nft.turbo.user.infrastructure.exception.UserErrorCode.*;

/**
 * 用户服务
 */
@Service
public class UserService extends ServiceImpl<UserMapper, User> implements InitializingBean {

    private static final String DEFAULT_NICK_NAME_PREFIX = "藏家_";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserOperateStreamService userOperateStreamService;
    @Autowired
    private AuthService authService;
    @Autowired
    private RedissonClient redissonClient;

    private RBloomFilter<String> nickNameBloomFilter;

    private RBloomFilter<String> inviteCodeBloomFilter;

    private RScoredSortedSet<String> inviteRank;

    @Autowired
    private CacheManager cacheManager;

    private Cache<String, User> idUserCache;
    private Cache<String, User> telUserCache;


    @PostConstruct
    public void init() {
        QuickConfig idQc = QuickConfig.newBuilder(":user:cache:id:")
                .cacheType(CacheType.BOTH)
                .expire(Duration.ofMinutes(3000))
                .syncLocal(true)
                .build();
        idUserCache = cacheManager.getOrCreateCache(idQc);

        QuickConfig telQc = QuickConfig.newBuilder(":user:cache:tel:")
                .cacheType(CacheType.BOTH)
                .expire(Duration.ofMinutes(3000))
                .syncLocal(true)
                .build();
        telUserCache = cacheManager.getOrCreateCache(telQc);
    }

    // 根据用户id查询用户（缓存）
    @Cached(name = ":user:cache:id:", cacheType = CacheType.BOTH, key = "#userId", cacheNullValue = true)
    @CacheRefresh(refresh = 60, timeUnit = TimeUnit.MINUTES)
    public User findById(Long userId) {
        return userMapper.findById(userId);
    }

    // 根据电话查询用户（缓存）
    @Cached(name = ":user:cache:tel:", cacheType = CacheType.BOTH, key = "#telephone", cacheNullValue = true)
    @CacheRefresh(refresh = 60, timeUnit = TimeUnit.MINUTES)
    public User findByTelephone(String telephone) {
        return userMapper.findByTelephone(telephone);
    }

    // 通过手机号和密码查询用户信息
    public User findByTelephoneAndPass(String telephone, String password) {
        return userMapper.findByTelephoneAndPass(telephone, DigestUtil.md5Hex(password));
    }

    // 记录并获取用户登录时间
    public Date getLoginTime(Long userId) {
        User user = findById(userId);
        // 1.获取用户上次登录时间
        Date lastLoginTime = user.getLastLoginTime();
        // 2.更新用户登录时间
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(User::getId, userId)
               .set(User::getLastLoginTime, new Date());
        update(user, wrapper);
        return lastLoginTime;
    }

    // 用户注册
    @DistributeLock(keyExpression = "#telephone", scene = "USER_REGISTER")
    public UserOperatorResponse register(String telephone, String inviteCode) {
        String defaultNickName;
        String randomString;
        // 生成邀请码、默认昵称
        do {
            randomString = RandomUtil.randomString(6).toUpperCase();
            defaultNickName = DEFAULT_NICK_NAME_PREFIX + randomString + telephone.substring(7, 11);
        } while (inviteCodeExist(randomString));

        String inviterId = null;
        if (StringUtils.isNotBlank(inviteCode)) {
            User inviter = userMapper.findByInviteCode(inviteCode);
            if (inviter != null) {
                inviterId = inviter.getId().toString();
            }
        }
        // 注册
        User user = register(telephone, defaultNickName, telephone, randomString, inviterId);
        Assert.notNull(user, USER_OPERATE_FAILED.getCode());
        // 更新邀请排名
        updateInviteRank(inviterId);
        // 更新布隆过滤器
        addNickName(defaultNickName);
        addInviteCode(inviteCode);
        // 更新用户缓存
        idUserCache.put(user.getId().toString(),user);
        telUserCache.remove(telephone);
        // 加入流水
        long streamResult = userOperateStreamService.insertStream(user, UserOperateTypeEnum.REGISTER);
        Assert.notNull(streamResult, () -> new BizException(RepoErrorCode.UPDATE_FAILED));

        UserOperatorResponse userOperatorResponse = new UserOperatorResponse();
        userOperatorResponse.setSuccess(true);
        userOperatorResponse.setUser(UserConvertor.INSTANCE.mapToVo(user));

        return userOperatorResponse;
    }

    // 用户注册-邀请码去重
    private boolean inviteCodeExist(String randomString) {
        if (this.inviteCodeBloomFilter != null && this.inviteCodeBloomFilter.contains(randomString)) {
            return userMapper.findByInviteCode(randomString) != null;
        }
        return false;
    }

    public boolean nickNameExist(String nickName) {
        //如果布隆过滤器中存在，再进行数据库二次判断
        if (this.nickNameBloomFilter != null && this.nickNameBloomFilter.contains(nickName)) {
            return userMapper.findByNickname(nickName) != null;
        }

        return false;
    }

    // 用户注册
    private User register(String telephone, String nickName, String password, String inviteCode, String inviterId) {
        if (userMapper.findByTelephone(telephone) != null) {
            throw new UserException(DUPLICATE_TELEPHONE_NUMBER);
        }
        User user = new User();
        user.register(telephone, nickName, password, inviteCode, inviterId);
        return save(user) ? user : null;
    }

    // 用户注册-将邀请码添加进布隆过滤器
    private boolean addInviteCode(String inviteCode) {
        return this.inviteCodeBloomFilter != null && this.inviteCodeBloomFilter.add(inviteCode);
    }

    // 用户注册-将昵称添加进布隆过滤器
    private boolean addNickName(String nickName) {
        return this.nickNameBloomFilter != null && this.nickNameBloomFilter.add(nickName);
    }

    // 用户注册-更新邀请排名
    private void updateInviteRank(String inviterId) {
        if (inviterId == null) {
            return;
        }
        RLock rLock = redissonClient.getLock(inviterId);
        rLock.lock();
        try {
            Double score = inviteRank.getScore(inviterId);
            if (score == null) {
                score = 0.0;
            }
            long timeStamp = System.currentTimeMillis();
            double newScore = score.intValue() + (1 - (double) timeStamp / 10000000000000L) + 100;
            inviteRank.add(newScore, inviterId);
        } finally {
            rLock.unlock();
        }
    }


    // 实名认证
    @Transactional(rollbackFor = Exception.class)
    @CacheInvalidate(name = ":user:cache:id:", key = "#userAuthRequest.userId")
    public UserOperatorResponse auth(UserAuthRequest userAuthRequest) {
        UserOperatorResponse response = new UserOperatorResponse();
        User user = findById(userAuthRequest.getUserId());

        Assert.notNull(user, () -> new UserException(USER_NOT_EXIST));
        if(user.getState() == UserStateEnum.AUTH || user.getState() == UserStateEnum.ACTIVE){
            response.setSuccess(true);
            response.setUser(UserConvertor.INSTANCE.mapToVo(user));
            return response;
        }
        Assert.isTrue(user.getState() == UserStateEnum.INIT, () -> new UserException(USER_STATUS_IS_NOT_INIT));

        Assert.isTrue(authService.checkAuth(userAuthRequest.getRealName(),userAuthRequest.getIdCard()));

        // 更新用户数据（真实姓名，身份证号，状态）
        user.auth(userAuthRequest.getRealName(),userAuthRequest.getIdCard());
        if(updateById(user)){
            // 添加用户操作流水
            Long streamResult = userOperateStreamService.insertStream(user,UserOperateTypeEnum.AUTH);
            if(null != streamResult){
                response.setSuccess(true);
                response.setUser(UserConvertor.INSTANCE.mapToVo(user));
                return response;
            }
        }
        response.setSuccess(false);
        response.setResponseCode(USER_AUTH_FAIL.getCode());
        response.setResponseMessage(USER_AUTH_FAIL.getMessage());
        return response;
    }

    // 管理员注册
    @DistributeLock(keyExpression = "#telephone", scene = "USER_REGISTER")
    public UserOperatorResponse registerAdmin(String telephone, String password) {
        User user = registerAdmin(telephone, telephone, password);
        Assert.notNull(user, USER_OPERATE_FAILED.getCode());
        idUserCache.put(user.getId().toString(), user);

        //加入流水
        long streamResult = userOperateStreamService.insertStream(user, UserOperateTypeEnum.REGISTER);
        Assert.notNull(streamResult, () -> new BizException(RepoErrorCode.UPDATE_FAILED));

        UserOperatorResponse userOperatorResponse = new UserOperatorResponse();
        userOperatorResponse.setSuccess(true);

        return userOperatorResponse;
    }

    private User registerAdmin(String telephone, String nickName, String password) {
        if (userMapper.findByTelephone(telephone) != null) {
            throw new UserException(DUPLICATE_TELEPHONE_NUMBER);
        }

        User user = new User();
        user.registerAdmin(telephone, nickName, password);
        return save(user) ? user:null;
    }
    // 实名认证

    // 上链后用户激活
    @Transactional
    @CacheInvalidate(name = ":user:cache:id:", key = "#request.userId")
    public UserOperatorResponse active(UserActiveRequest request) {
        UserOperatorResponse response = new UserOperatorResponse();
        User user = userMapper.findById(Long.parseLong(request.getUserId()));
        user.active(request.getBlockChainUrl(), request.getBlockChainPlatform());
        if(updateById(user)){
            // 加入流水
            Long streamResult = userOperateStreamService.insertStream(user, UserOperateTypeEnum.ACTIVE);
            Assert.notNull(streamResult, () -> new BizException(RepoErrorCode.UPDATE_FAILED));
            response.setSuccess(true);
            return response;
        }
        response.setSuccess(false);
        response.setResponseCode(USER_OPERATE_FAILED.getCode());
        response.setResponseMessage(USER_OPERATE_FAILED.getMessage());
        return response;
    }

    // 冻结
    @Transactional(rollbackFor = Exception.class)
    public UserOperatorResponse freeze(Long userId) {
        //查询用户

        //第一次删除缓存

        //更新数据库

        //加入流水

        //第二次删除缓存
        return null;
    }

    // 解冻
    @Transactional(rollbackFor = Exception.class)
    public UserOperatorResponse unfreeze(Long userId) {
        return null;
    }

    // 分页查询用户信息
    public PageResponse<User> pageQueryByState(String keyWord, String state, int currentPage, int pageSize) {
        Page<User> page = new Page<>(currentPage, pageSize);
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("state", state);

        if (keyWord != null) {
            wrapper.like("telephone", keyWord);
        }
        wrapper.orderBy(true, true, "gmt_create");

        Page<User> userPage = this.page(page, wrapper);

        return PageResponse.of(userPage.getRecords(), (int) userPage.getTotal(), pageSize, currentPage);
    }


    // 修改用户信息
    public UserOperatorResponse modify(UserModifyRequest request){
        UserOperatorResponse response = new UserOperatorResponse();
        User user = findById(request.getUserId());
        // 账号是否存在
        Assert.notNull(user,() -> new UserException(USER_NOT_EXIST));
        // 账号是否正常
        Assert.isTrue(user.canModifyInfo(), () -> new UserException(USER_STATUS_CANT_OPERATE));
        // 昵称是否重复
        if(StringUtils.isNotBlank(request.getNickName()) && nickNameExist(request.getNickName())){
            throw new UserException(NICK_NAME_EXIST);
        }
        // 修改用户信息
        BeanUtils.copyProperties(request,user);
        // 修改密码
        if(StringUtils.isNotBlank(request.getPassword())){
            user.setPasswordHash(DigestUtil.md5Hex(request.getPassword()));
        }
        if(updateById(user)){
            // 加入流水
            Long streamResult = userOperateStreamService.insertStream(user, UserOperateTypeEnum.MODIFY);
            Assert.notNull(streamResult, () -> new BizException(RepoErrorCode.UPDATE_FAILED));
            // 更新布隆过滤器
            addNickName(request.getNickName());
            // 删除缓存
            idUserCache.remove(user.getId().toString());
            response.setSuccess(true);
            return response;
        }
        response.setSuccess(false);
        response.setResponseCode(USER_OPERATE_FAILED.getCode());
        response.setResponseMessage(USER_OPERATE_FAILED.getMessage());
        return response;
    }


    // 获取邀请排行榜topN
    public List<InviteRankInfo> getTopN(@Max(100) Integer topN) {
        Collection<ScoredEntry<String>> rankInfos = inviteRank.entryRangeReversed(0,topN-1);
        List<InviteRankInfo> inviteRankInfos = new ArrayList<>();
        if(rankInfos != null){
            for(ScoredEntry<String> rankInfo : rankInfos){
                InviteRankInfo inviteRankInfo = new InviteRankInfo();
                String userId = rankInfo.getValue();
                if(StringUtils.isNotBlank(userId)){
                    User user = findById(Long.valueOf(userId));
                    if (user != null) {
                        inviteRankInfo.setInviteCode(user.getInviteCode());
                        inviteRankInfo.setInviteCount(rankInfo.getScore().intValue()/100);
                        inviteRankInfo.setNickName(user.getNickName());
                        inviteRankInfos.add(inviteRankInfo);
                    }
                }
            }
        }
        return inviteRankInfos;
    }

    // 获取当前用户邀请排名
    public Integer getInvteRank(String userId) {
        Integer rank = inviteRank.revRank(userId);
        if(rank == null){
            return 0;
        }
        return rank + 1;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.nickNameBloomFilter = redissonClient.getBloomFilter("nickName");
        if (nickNameBloomFilter != null && !nickNameBloomFilter.isExists()) {
            this.nickNameBloomFilter.tryInit(100000L, 0.01);
        }

        this.inviteCodeBloomFilter = redissonClient.getBloomFilter("inviteCode");
        if (inviteCodeBloomFilter != null && !inviteCodeBloomFilter.isExists()) {
            this.inviteCodeBloomFilter.tryInit(100000L, 0.01);
        }

        this.inviteRank = redissonClient.getScoredSortedSet("inviteRank");
    }
}
