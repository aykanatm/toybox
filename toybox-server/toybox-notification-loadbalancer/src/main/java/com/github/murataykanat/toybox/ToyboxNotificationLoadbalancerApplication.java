package com.github.murataykanat.toybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableEurekaClient
@EnableCircuitBreaker
@EnableRetry
public class ToyboxNotificationLoadbalancerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyboxNotificationLoadbalancerApplication.class, args);
    }

}
