package com.example.metrics.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API安全拦截器
 *
 * <p>验证HTTP请求的来源和路径安全性：
 * <ul>
 *   <li>检查请求来源（Origin）是否在白名单中</li>
 *   <li>检查请求路径是否在白名单中</li>
 * </ul>
 *
 * <p>配置项：
 * <ul>
 *   <li>security.cors.allowed-origins: 允许的来源列表，逗号分隔</li>
 *   <li>security.url.whitelist: 路径白名单，逗号分隔</li>
 * </ul>
 */
@Component
public class ApiSecurityInterceptor implements HandlerInterceptor {

    @Value("${security.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${security.url.whitelist:}")
    private String urlWhitelist;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String origin = request.getHeader("Origin");
        String requestUri = request.getRequestURI();

        if (isUrlWhitelisted(requestUri)) {
            return true;
        }

        if (!isOriginAllowed(origin)) {
            sendError(response, HttpStatus.FORBIDDEN, "不允许的来源: " + origin);
            return false;
        }

        return true;
    }

    private boolean isUrlWhitelisted(String uri) {
        if (!StringUtils.hasText(urlWhitelist)) {
            return false;
        }
        List<String> whitelist = parseList(urlWhitelist);
        return whitelist.stream().anyMatch(uri::startsWith);
    }

    private boolean isOriginAllowed(String origin) {
        if (!StringUtils.hasText(allowedOrigins) || "*".equals(allowedOrigins)) {
            return true;
        }
        if (origin == null) {
            return true;
        }
        List<String> allowedList = parseList(allowedOrigins);
        return allowedList.contains(origin);
    }

    private List<String> parseList(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
