package com.example.tennis_club.dtos.reservation;

import com.example.tennis_club.models.GameType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservationRequest(
        @NotBlank
        String courtNumber,

        @NotBlank
        String customerPhoneNumber,

        @NotBlank
        String customerName,

        @NotNull
        LocalDateTime startTime,

        @NotNull
        LocalDateTime endTime,

        @NotNull
        GameType gameType
) {
}
