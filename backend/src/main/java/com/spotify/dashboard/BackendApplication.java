package com.spotify.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication // backbone of the whole Spring Boot engine
@EnableCaching // enables Spring's caching abstraction
public class BackendApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args); 
        // enables auto-configuration, scans for components, and sets up application context
    }
}