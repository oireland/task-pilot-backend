package com.oireland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.oireland.config")
public class TaskExtractionAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskExtractionAppApplication.class, args);
    }

}
