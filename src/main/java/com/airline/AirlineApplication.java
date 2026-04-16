package com.airline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirlineApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("  Skape Airways Reservation System Started!");
        System.out.println("  Open: http://localhost:8080");
        System.out.println("========================================\n");
    }
}
