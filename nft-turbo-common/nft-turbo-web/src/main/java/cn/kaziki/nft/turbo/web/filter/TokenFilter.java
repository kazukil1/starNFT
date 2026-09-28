package cn.kaziki.nft.turbo.web.filter;

import cn.kaziki.nft.turbo.web.util.TokenUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.util.UUID;

/**
 * token拦截器，校验下单
 */

public class TokenFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TokenFilter.class);

    public static final ThreadLocal<String> TOKEN_THREAD_LOCAL = new ThreadLocal<>();

    public static final ThreadLocal<Boolean> STRESS_THREAD_LOCAL = new ThreadLocal<>();

    private static final String HEADER_VALUE_NULL = "null";

    private static final String HEADER_VALUE_UNDEFINED = "undefined";

    private StringRedisTemplate stringRedisTemplate;

    public TokenFilter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 过滤器初始化，可选实现
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 从请求头中获取Token
            String token = httpRequest.getHeader("Authorization");
            Boolean isStress = BooleanUtils.toBoolean(httpRequest.getHeader("isStress"));

            if (token == null || HEADER_VALUE_NULL.equals(token) || HEADER_VALUE_UNDEFINED.equals(token)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("No Token Found ...");
                logger.error("no token found in header , pls check!");
                return;
            }

            // 校验Token的有效性
            boolean isValid = checkTokenValidity(token, isStress);

            if (!isValid) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.getWriter().write("Invalid or expired token");
                logger.error("token validate failed , pls check!");
                return;
            }

            // Token有效，继续执行其他过滤器链
            chain.doFilter(request, response);
        } finally {
            TOKEN_THREAD_LOCAL.remove();
            STRESS_THREAD_LOCAL.remove();
        }
    }

    /**
     * 1、把加密后的token解密得到key
     * 2、用 key 去 redis 做 GETDEL 获取存储的 token
     * 3、比较存储的 token 和请求传入的 token 是否一致
     * 4、一致则校验通过，key 已被删除（天然防重）
     */
    private boolean checkTokenValidity(String token, Boolean isStress) {
        String result;
        if (isStress) {
            // 如果是压测，则生成一个随机数，模拟 token
            result = UUID.randomUUID().toString();
            STRESS_THREAD_LOCAL.set(isStress);
        } else {
            String tokenKey = TokenUtil.getTokenKeyByValue(token);

            try {
                // 1. GETDEL：获取并删除，天然防重（对比 Lua 脚本更安全，不会出现 codec 编码问题）
                // 2. 删除成功 → token 有效；删除失败（没有删除成功）→ token 无效
                Boolean deleted = stringRedisTemplate.delete(tokenKey);

                if (Boolean.TRUE.equals(deleted)) {
                    result = token;
                } else {
                    logger.error("token validate failed, key not found: {}", tokenKey);
                    return false;
                }
            } catch (Exception e) {
                logger.error("check token failed", e);
                return false;
            }
        }

        TOKEN_THREAD_LOCAL.set(result);
        return result != null;
    }

    @Override
    public void destroy() {
    }
}