package com.tickets.managementtickets;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ManagementTicketsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagementTicketsApplication.class, args);
    }
}
