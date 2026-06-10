package com.example.tennis_club.dtos.surface;

import java.math.BigDecimal;

public record SurfaceTypeResponse(
        Long id,
        String name,
        BigDecimal pricePerMinute) {
}
