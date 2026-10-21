package com.payments.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication: Ye Spring Boot ka main trigger hai.
// Iske andar 3 main cheezein hoti hain:
// 1. @Configuration (Java configuration class banata hai)
// 2. @EnableAutoConfiguration (Classpath ke basis pe automatically libraries configure karta hai, jaise DB, Tomcat)
// 3. @ComponentScan (Is package aur sub-packages me saare @Component, @Service, @Repository dhundta hai)
@SpringBootApplication
public class AccountServiceApplication {

	public static void main(String[] args) {
		// Embedded Tomcat server start karega aur application context load karega
		SpringApplication.run(AccountServiceApplication.class, args);
	}

}
