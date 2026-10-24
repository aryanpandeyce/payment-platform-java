package com.payments.fraud.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fraudServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fraud Service API")
                        .version("1.0.0")
                        .description("Real-time rule engine detecting payment anomalies, transaction amount caps, and velocity breaches.")
                        .contact(new Contact().name("Risk & Fraud Engineering")));
    }
}
