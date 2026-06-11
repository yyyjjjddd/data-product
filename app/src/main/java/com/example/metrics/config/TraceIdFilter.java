package com.example.metrics.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId生成过滤器
 *
 * <p>为每个HTTP请求生成唯一的traceId，用于链路追踪：
 * <ul>
 *   <li>生成规则：{METHOD}_{PATH}_{UUID后8位}</li>
 *   <li>示例：POST_api_v1_tasks_a1b2c3d4</li>
 *   <li>存储在请求属性中，供后续使用</li>
 * </ul>
 *
 * <p>将traceId设置到响应头中，便于客户端排查问题。
 */
public class TraceIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_ATTRIBUTE = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = generateTraceId(httpRequest);
        httpRequest.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        log.debug("TraceId generated: {}", traceId);
        chain.doFilter(request, response);
    }

    /**
     * 生成traceId
     *
     * <p>格式：{METHOD}_{PATH}_{UUID后8位}
     * <ul>
     *   <li>METHOD: HTTP方法，如GET、POST</li>
     *   <li>PATH: API路径，将/替换为_，去除前缀/</li>
     *   <li>UUID后8位: 保证唯一性</li>
     * </ul>
     *
     * <p>示例：POST_api_v1_tasks_a1b2c3d4
     */
    private String generateTraceId(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // 简化路径：将/替换为_，去除开头的/
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        path = path.replace("/", "_");

        // 生成UUID后8位
        String uuidSuffix = UUID.randomUUID().toString().replace("-", "").substring(24);

        return method + "_" + path + "_" + uuidSuffix;
    }
}
