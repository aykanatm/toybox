package com.github.murataykanat.toybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.turbine.stream.EnableTurbineStream;

@SpringBootApplication
@EnableTurbineStream
public class ToyboxHystrixTurbineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToyboxHystrixTurbineApplication.class, args);
	}
}
