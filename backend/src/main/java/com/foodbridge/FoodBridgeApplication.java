package com.foodbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FoodBridge - food donation and distribution coordination platform.
 *
 * <p>Modular monolith: each business capability lives in its own package
 * ({@code auth}, {@code user}, {@code donation}, {@code claim}, {@code pickup},
 * {@code notification}, {@code admin}) and communicates through services.</p>
 */
// Authentication is stateless JWT, so Spring Security's generated in-memory
// user (and its random startup password in the logs) serves no purpose.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class FoodBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodBridgeApplication.class, args);
    }
}
