package com.example.tennis_club.dtos.court;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CourtRequest(
        @NotBlank
        String courtNumber,

        @NotNull
        Long surfaceTypeId
) {
}
