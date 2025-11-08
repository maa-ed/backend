package com.firomsa.maaedBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MaaedBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaaedBackendApplication.class, args);
	}

}
