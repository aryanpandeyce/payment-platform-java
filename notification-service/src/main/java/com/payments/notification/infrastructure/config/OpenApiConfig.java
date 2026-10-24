package com.payments.notification.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notification & Audit Service API")
                        .version("1.0.0")
                        .description("Microservice consuming domain events, logging customer alerts, and persisting immutable append-only audit trails into MongoDB.")
                        .contact(new Contact().name("Compliance & Auditing Team")));
    }
}
