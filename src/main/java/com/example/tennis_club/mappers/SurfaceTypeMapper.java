package com.example.tennis_club.mappers;

import com.example.tennis_club.dtos.surface.SurfaceTypeResponse;
import com.example.tennis_club.entities.SurfaceType;

public final class SurfaceTypeMapper {
    public static SurfaceTypeResponse toSurfaceTypeResponse(SurfaceType surfaceType) {
        return new SurfaceTypeResponse(
                surfaceType.getId(),
                surfaceType.getName(),
                surfaceType.getPricePerMinute());
    }
}
