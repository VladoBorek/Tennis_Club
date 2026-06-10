package com.example.tennis_club.dtos.court;

import java.math.BigDecimal;

public record CourtResponse(
        Long id,
        String courtNumber,
        Long surfaceTypeId,
        String surfaceTypeName,
        BigDecimal pricePerMinute
) {
}
