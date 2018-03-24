package com.github.murataykanat.toybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;

@SpringBootApplication
@EnableHystrixDashboard
public class ToyboxHystrixDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToyboxHystrixDashboardApplication.class, args);
	}
}
