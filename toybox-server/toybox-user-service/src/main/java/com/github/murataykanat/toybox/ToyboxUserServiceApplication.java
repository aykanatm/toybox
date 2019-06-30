package com.github.murataykanat.toybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class ToyboxUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToyboxUserServiceApplication.class, args);
    }

}
