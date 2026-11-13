package com.musix.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot Application Entry Point for MUSIX
 * For running via Maven: mvn spring-boot:run
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.musix"})
public class MusixSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusixSpringBootApplication.class, args);
    }
}
