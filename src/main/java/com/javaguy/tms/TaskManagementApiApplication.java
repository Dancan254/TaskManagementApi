package com.javaguy.tms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TaskManagementApiApplication {

    static void main(String[] args) {
        SpringApplication.run(TaskManagementApiApplication.class, args);
    }

}
