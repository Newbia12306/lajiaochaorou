package com.example.demo.config;

import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.example.demo.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        try {
            String hostPort = elasticsearchUris
                    .replace("https://", "")
                    .replace("http://", "");

            boolean useSsl = elasticsearchUris.startsWith("https://");
            boolean useAuth = username != null && !username.isBlank()
                    && password != null && !password.isBlank();

            SSLContext sslContext = null;
            if (useSsl) {
                sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial((chain, authType) -> true)
                        .build();
            }

            // 由于 ClientConfiguration 使用 step-builder 模式，
            // 必须根据条件走不同分支，无法用中间变量存储
            if (useSsl && useAuth) {
                return ClientConfiguration.builder()
                        .connectedTo(hostPort)
                        .usingSsl(sslContext)
                        .withBasicAuth(username, password)
                        .build();
            } else if (useSsl) {
                return ClientConfiguration.builder()
                        .connectedTo(hostPort)
                        .usingSsl(sslContext)
                        .build();
            } else if (useAuth) {
                return ClientConfiguration.builder()
                        .connectedTo(hostPort)
                        .withBasicAuth(username, password)
                        .build();
            } else {
                return ClientConfiguration.builder()
                        .connectedTo(hostPort)
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Elasticsearch client configuration", e);
        }
    }
}
