package com.example.tennis_club.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.data-initialization")
public class DataInitializationProperties {

    private boolean enabled;
}