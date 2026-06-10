package com.example.tennis_club.mappers;

import com.example.tennis_club.dtos.court.CourtResponse;
import com.example.tennis_club.entities.Court;

import java.util.List;

public final class CourtMapper {

    public static CourtResponse toResponse(Court court) {
        return new CourtResponse(
                court.getId(),
                court.getCourtNumber(),
                court.getSurfaceType().getId(),
                court.getSurfaceType().getName(),
                court.getSurfaceType().getPricePerMinute()
        );
    }

    public static List<CourtResponse> toResponseList(List<Court> courts) {
        return courts.stream().map(CourtMapper::toResponse).toList();
    }

}
