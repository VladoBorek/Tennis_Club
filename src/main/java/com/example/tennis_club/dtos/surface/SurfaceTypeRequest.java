package com.example.tennis_club.dtos.surface;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SurfaceTypeRequest(
        @NotBlank
        String name,
        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal pricePerMinute) {
}

