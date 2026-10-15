package com.examradar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExamRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamRadarApplication.class, args);
    }
}
