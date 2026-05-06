package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        // 1. 从环境变量读取（Docker部署时使用）
        String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
        String username = System.getenv("SPRING_DATASOURCE_USERNAME");
        String password = System.getenv("SPRING_DATASOURCE_PASSWORD");

        // 2. 如果没有环境变量，使用默认值（本地开发用）
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "jdbc:postgresql://localhost:5432/demo1";
        }
        if (username == null || username.isEmpty()) {
            username = "postgres";
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException(
                "未配置数据库密码！请设置环境变量 SPRING_DATASOURCE_PASSWORD 或创建 .env 文件"
            );
        }

        // 3. 打印调试信息（Docker部署时查看）
        System.out.println("=== 数据库连接配置 ===");
        System.out.println("URL: " + dbUrl);
        System.out.println("Username: " + username);
        System.out.println("=========================");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}