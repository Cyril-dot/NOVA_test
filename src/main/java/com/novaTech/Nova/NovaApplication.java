package com.novaTech.Nova;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class NovaApplication {

	public static void main(String[] args) {
		SpringApplication.run(NovaApplication.class, args);
	}
}
