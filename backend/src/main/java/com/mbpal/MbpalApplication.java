package com.mbpal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MbpalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MbpalApplication.class, args);
    }
}
