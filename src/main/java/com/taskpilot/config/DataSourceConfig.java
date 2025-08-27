package com.taskpilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String jdbcUsername;

    @Value("${spring.datasource.password}")
    private String jdbcPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new RuntimeException("Database URL is not configured. Please set spring.datasource.url.");
        }

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(jdbcUsername)
                .password(jdbcPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}