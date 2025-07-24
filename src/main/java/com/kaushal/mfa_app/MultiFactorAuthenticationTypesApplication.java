package com.kaushal.mfa_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MultiFactorAuthenticationTypesApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultiFactorAuthenticationTypesApplication.class, args);
		System.out.println("MFA service active on port: 8080");
	}

}
