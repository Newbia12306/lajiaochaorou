package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.demo.filter.JwtAuthenticationFilter;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 配置跨域
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. 关闭CSRF
                .csrf(csrf -> csrf.disable())
                // 3. 配置接口访问权限
                .authorizeHttpRequests(auth -> auth
                        // 公开接口
                        .requestMatchers("/api/auth/**").permitAll()  // 所有认证相关接口都公开
                        .requestMatchers("/api/recommendations/**").permitAll()  // 推荐接口公开
                        .requestMatchers("/api/dishes/**").permitAll()  // 菜品接口公开（菜单浏览）
                        .requestMatchers("/api/llm/**").authenticated()  // LLM接口需要登录（个性化推荐）

                        .requestMatchers("/login.html", "/register.html").permitAll()
                        // 静态资源
                        .requestMatchers("/", "/static/**", "/webapp/**",  "/*.js", "/*.css").permitAll()

                        // 保护所有 HTML 页面（除了登录注册页）
                        .requestMatchers("/*.html").authenticated()

                        // 其他所有接口需要认证
                        .anyRequest().authenticated()
                )
                // 4. 添加JWT过滤器
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 配置跨域规则：允许前端域名、请求方法、请求头
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源，可以根据需要设置具体的前端地址
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        // 或者指定具体域名：
        // configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许携带凭证（如cookies）
        configuration.setAllowCredentials(true);

        // 预检请求的缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用CORS配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}