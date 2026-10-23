package com.swayam.pathprep;

import com.swayam.pathprep.config.AdminProperties;
import com.swayam.pathprep.config.AuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AdminProperties.class, AuthProperties.class})
public class PathprepApplication {

    public static void main(String[] args) {
        log.info("Starting PathPrep backend application...");
        SpringApplication.run(PathprepApplication.class, args);
        log.info("PathPrep application started successfully.");
    }

}
