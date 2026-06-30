package com.example.demo.filter;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 🔥 新增：跳过公开页面和资源的 JWT 验证
        String requestURI = request.getRequestURI();
        if (shouldSkipJwtValidation(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取请求头中的Authorization
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String mobileNumber;

        // 验证Authorization格式（Bearer + 令牌）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取JWT令牌
        jwt = authHeader.substring(7);
        try {
            // 从令牌中提取手机号
            mobileNumber = jwtService.extractMobileNumber(jwt);

            // 如果令牌有效且当前上下文未认证
            if (mobileNumber != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 验证令牌有效性
                if (jwtService.validateToken(jwt)) {
                    // 创建认证对象并设置到上下文
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            mobileNumber,
                            null,
                            null
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token is invalid or expired, clear the security context
            log.debug("JWT token validation failed, clearing security context: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    // 🔥 新增：判断是否需要跳过 JWT 验证
    private boolean shouldSkipJwtValidation(String requestURI) {
        // 跳过登录、注册页面
        if (requestURI.equals("/login.html") || requestURI.equals("/register.html")) {
            return true;
        }

        // 跳过认证接口
        if (requestURI.startsWith("/api/auth/")) {
            return true;
        }

        // 跳过静态资源
        if (requestURI.startsWith("/static/") ||
                requestURI.startsWith("/webapp/") ||
                requestURI.endsWith(".js") ||
                requestURI.endsWith(".css")) {
            return true;
        }

        // 跳过根路径
        if (requestURI.equals("/")) {
            return true;
        }

        return false;
    }
}