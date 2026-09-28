package cn.kaziki.nft.turbo.web.configuration;

import cn.kaziki.nft.turbo.web.filter.TokenFilter;
import cn.kaziki.nft.turbo.web.handler.GlobalWebExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@AutoConfiguration
@ConditionalOnWebApplication
public class WebConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    GlobalWebExceptionHandler globalWebExceptionHandler() {
        return new GlobalWebExceptionHandler();
    }

    /**
     * 注册token过滤器
     *
     * @param stringRedisTemplate
     * @return
     */
    @Bean
    public FilterRegistrationBean<TokenFilter> tokenFilter(StringRedisTemplate stringRedisTemplate) {
        FilterRegistrationBean<TokenFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new TokenFilter(stringRedisTemplate));
        registrationBean.addUrlPatterns("/trade/newBuy");
        registrationBean.addUrlPatterns("/trade/normalBuy");
        registrationBean.setOrder(10);

        return registrationBean;
    }

}
