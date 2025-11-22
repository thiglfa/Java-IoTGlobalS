package com.wellwork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class WellWorkApplication {
    public static void main(String[] args) {
        SpringApplication.run(WellWorkApplication.class, args);
    }
}
