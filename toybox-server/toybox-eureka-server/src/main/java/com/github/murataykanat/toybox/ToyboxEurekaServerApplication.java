package com.github.murataykanat.toybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class ToyboxEurekaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToyboxEurekaServerApplication.class, args);
	}
}
