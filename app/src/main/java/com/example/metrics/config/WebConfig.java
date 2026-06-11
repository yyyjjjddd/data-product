package com.example.metrics.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 *
 * <p>配置Web相关组件：
 * <ul>
 *   <li>注册API安全拦截器</li>
 *   <li>注册TraceId过滤器</li>
 * </ul>
 *
 * @see ApiSecurityInterceptor
 * @see TraceIdFilter
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiSecurityInterceptor apiSecurityInterceptor;

    public WebConfig(ApiSecurityInterceptor apiSecurityInterceptor) {
        this.apiSecurityInterceptor = apiSecurityInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiSecurityInterceptor)
                .addPathPatterns("/api/**");
    }

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        registration.setOrder(1);
        return registration;
    }
}
