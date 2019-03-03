package com.github.murataykanat.toybox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.cloud.netflix.turbine.EnableTurbine;

@EnableTurbine
@SpringBootApplication
@EnableHystrixDashboard
public class ToyboxHystrixDashboardApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToyboxHystrixDashboardApplication.class, args);
	}
}
