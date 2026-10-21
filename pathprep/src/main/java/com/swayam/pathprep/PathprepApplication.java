package com.swayam.pathprep;

import com.swayam.pathprep.config.AdminProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(AdminProperties.class)
public class PathprepApplication {

    public static void main(String[] args) {
        log.info("Starting PathPrep backend application...");
        SpringApplication.run(PathprepApplication.class, args);
        log.info("PathPrep application started successfully.");
    }

}
