package com.taskpilot.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile("!dev") // Only use this configuration when NOT in the 'dev' profile
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() throws URISyntaxException {
        // Heroku provides the database connection details in a single URL
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null) {
            throw new RuntimeException("DATABASE_URL environment variable is not set.");
        }

        URI dbUri = new URI(databaseUrl);

        String username = dbUri.getUserInfo().split(":")[0];
        String password = dbUri.getUserInfo().split(":")[1];
        // Construct the JDBC URL from the Heroku URI
        String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .username(username)
                .password(password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }
}