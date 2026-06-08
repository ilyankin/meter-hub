package com.ilynkin.coding_assignment;

import org.springframework.boot.SpringApplication;

public class TestCodingAssignmentApplication {

	public static void main(String[] args) {
		SpringApplication.from(MeterHubApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
