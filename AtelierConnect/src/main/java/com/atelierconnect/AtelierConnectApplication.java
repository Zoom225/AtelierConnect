package com.atelierconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtelierConnectApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtelierConnectApplication.class, args);
	}

}
