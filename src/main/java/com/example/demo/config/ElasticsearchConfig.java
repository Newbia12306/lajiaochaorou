package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.example.demo.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {
        String hostPort = elasticsearchUris
                .replace("https://", "")
                .replace("http://", "");

        boolean useSsl = elasticsearchUris.startsWith("https://");
        boolean useAuth = username != null && !username.isBlank()
                && password != null && !password.isBlank();

        // Builder uses a step-builder pattern; must branch on conditions
        if (useSsl && useAuth) {
            return ClientConfiguration.builder()
                    .connectedTo(hostPort)
                    .usingSsl(getDefaultSslContext())
                    .withBasicAuth(username, password)
                    .build();
        } else if (useSsl) {
            return ClientConfiguration.builder()
                    .connectedTo(hostPort)
                    .usingSsl(getDefaultSslContext())
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
    }

    /**
     * Use the JVM's default SSL context which validates certificates against
     * the trusted CA store, instead of trusting all certificates blindly.
     */
    private SSLContext getDefaultSslContext() {
        try {
            return SSLContext.getDefault();
        } catch (Exception e) {
            log.error("Failed to obtain default SSL context for Elasticsearch", e);
            throw new IllegalStateException(
                    "Failed to obtain default SSL context for Elasticsearch connection", e);
        }
    }
}
