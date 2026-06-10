package com.example.tennis_club.dtos.reservation;

import com.example.tennis_club.models.GameType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        String courtNumber,
        Long surfaceTypeId,
        String surfaceTypeName,
        String customerPhoneNumber,
        String customerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        GameType gameType,
        BigDecimal price,
        LocalDateTime createdAt
) {
}