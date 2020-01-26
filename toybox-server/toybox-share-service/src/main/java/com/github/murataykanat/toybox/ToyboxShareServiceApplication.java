package com.github.murataykanat.toybox;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableEurekaClient
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
public class ToyboxShareServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyboxShareServiceApplication.class, args);
    }

}
