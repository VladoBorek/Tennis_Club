package com.example.tennis_club.services;

import com.example.tennis_club.daos.CourtDao;
import com.example.tennis_club.daos.SurfaceTypeDao;
import com.example.tennis_club.dtos.surface.SurfaceTypeRequest;
import com.example.tennis_club.dtos.surface.SurfaceTypeResponse;
import com.example.tennis_club.entities.SurfaceType;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import com.example.tennis_club.mappers.SurfaceTypeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class SurfaceTypeService {

    private final SurfaceTypeDao surfaceTypeDao;
    private final CourtDao courtDao;

    public SurfaceTypeService(SurfaceTypeDao surfaceTypeDao, CourtDao courtDao) {
        this.surfaceTypeDao = surfaceTypeDao;
        this.courtDao = courtDao;
    }

    @Transactional(readOnly = true)
    public List<SurfaceTypeResponse> getAllSurfaceTypes() {
        return surfaceTypeDao.findAllActive()
                .stream()
                .map(SurfaceTypeMapper::toSurfaceTypeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SurfaceTypeResponse getSurfaceTypeById(Long id) {
        SurfaceType surfaceType = getActiveSurfaceTypeById(id);
        return SurfaceTypeMapper.toSurfaceTypeResponse(surfaceType);
    }

    public SurfaceTypeResponse createSurfaceType(SurfaceTypeRequest request) {
        validateSurfaceTypeNameIsUnique(request.name());

        SurfaceType surfaceType = new SurfaceType();
        surfaceType.setName(request.name());
        surfaceType.setPricePerMinute(request.pricePerMinute());

        SurfaceType savedSurfaceType = surfaceTypeDao.save(surfaceType);
        return SurfaceTypeMapper.toSurfaceTypeResponse(savedSurfaceType);
    }

    public SurfaceTypeResponse updateSurfaceType(Long id, SurfaceTypeRequest request) {
        SurfaceType surfaceType = getActiveSurfaceTypeById(id);

        if (!Objects.equals(surfaceType.getName(), request.name())) {
            validateSurfaceTypeNameIsUnique(request.name());
        }

        surfaceType.setName(request.name());
        surfaceType.setPricePerMinute(request.pricePerMinute());

        SurfaceType savedSurfaceType = surfaceTypeDao.save(surfaceType);
        return SurfaceTypeMapper.toSurfaceTypeResponse(savedSurfaceType);
    }

    public void deleteSurfaceType(Long id) {
        SurfaceType surfaceType = getActiveSurfaceTypeById(id);

        if (courtDao.existsActiveBySurfaceTypeId(id)) {
            throw new BadRequestException("Surface type is used by active courts");
        }

        surfaceTypeDao.softDelete(surfaceType);
    }

    private SurfaceType getActiveSurfaceTypeById(Long id) {
        return surfaceTypeDao.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Surface type not found"));
    }


    private void validateSurfaceTypeNameIsUnique(String name) {
        surfaceTypeDao.findActiveByName(name)
                .ifPresent(surfaceType -> {
                    throw new BadRequestException("Surface type name already exists");
                });
    }

}
