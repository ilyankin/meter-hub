package com.ilynkin.coding_assignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MeterHubApplication {

	public static void main(String[] args) {
		SpringApplication.run(MeterHubApplication.class, args);
	}

}
