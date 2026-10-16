package com.manhnv.capstonebakerai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CapstoneBakerAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapstoneBakerAiApplication.class, args);
    }

}
