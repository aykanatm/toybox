package com.github.murataykanat.toybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class ToyboxRenditionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyboxRenditionServiceApplication.class, args);
    }

}

