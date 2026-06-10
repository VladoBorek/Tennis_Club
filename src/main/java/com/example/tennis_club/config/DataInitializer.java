package com.example.tennis_club.config;

import com.example.tennis_club.dtos.court.CourtRequest;
import com.example.tennis_club.dtos.surface.SurfaceTypeRequest;
import com.example.tennis_club.dtos.surface.SurfaceTypeResponse;
import com.example.tennis_club.services.CourtService;
import com.example.tennis_club.services.SurfaceTypeService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@EnableConfigurationProperties(DataInitializationProperties.class)
public class DataInitializer {

    @Bean
    public ApplicationRunner initializeData(
            DataInitializationProperties properties,
            SurfaceTypeService surfaceTypeService,
            CourtService courtService
    ) {
        return args -> {
            if (!properties.isEnabled() || !surfaceTypeService.getAllSurfaceTypes().isEmpty()) {
                return;
            }

            SurfaceTypeResponse clay = surfaceTypeService.createSurfaceType(
                    new SurfaceTypeRequest("Clay", new BigDecimal("0.30"))
            );

            SurfaceTypeResponse grass = surfaceTypeService.createSurfaceType(
                    new SurfaceTypeRequest("Grass", new BigDecimal("0.45"))
            );

            List<CourtRequest> courts = List.of(
                    new CourtRequest("COURT-1", clay.id()),
                    new CourtRequest("COURT-2", clay.id()),
                    new CourtRequest("COURT-3", grass.id()),
                    new CourtRequest("COURT-4", grass.id())
            );

            courts.forEach(courtService::createCourt);
        };
    }
}