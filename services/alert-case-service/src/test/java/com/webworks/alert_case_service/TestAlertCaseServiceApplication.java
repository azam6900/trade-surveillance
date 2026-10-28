package com.webworks.alert_case_service;

import org.springframework.boot.SpringApplication;

public class TestAlertCaseServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(AlertCaseServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
