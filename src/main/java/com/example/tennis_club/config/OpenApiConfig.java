package com.example.tennis_club.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Tennis Club API",
                version = "1.0.0",
                description = "REST API for managing tennis courts, surface types, and reservations."
        )
)
public class OpenApiConfig {
}