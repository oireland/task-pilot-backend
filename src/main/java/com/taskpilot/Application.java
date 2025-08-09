package com.taskpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.taskpilot.config")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
