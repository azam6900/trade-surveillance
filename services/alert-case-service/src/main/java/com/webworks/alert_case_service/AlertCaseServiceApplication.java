package com.webworks.alert_case_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlertCaseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlertCaseServiceApplication.class, args);
	}

}