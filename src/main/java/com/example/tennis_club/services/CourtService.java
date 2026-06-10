package com.example.tennis_club.services;


import com.example.tennis_club.daos.CourtDao;
import com.example.tennis_club.daos.SurfaceTypeDao;
import com.example.tennis_club.dtos.court.CourtRequest;
import com.example.tennis_club.dtos.court.CourtResponse;
import com.example.tennis_club.entities.Court;
import com.example.tennis_club.entities.SurfaceType;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import com.example.tennis_club.mappers.CourtMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourtService {

    private final CourtDao courtDao;
    private final SurfaceTypeDao surfaceTypeDao;

    public CourtService(CourtDao courtDao, SurfaceTypeDao surfaceTypeDao) {
        this.courtDao = courtDao;
        this.surfaceTypeDao = surfaceTypeDao;
    }

    @Transactional(readOnly = true)
    public List<CourtResponse> getAllCourts() {
        return CourtMapper.toResponseList(courtDao.findAllActive());
    }

    @Transactional(readOnly = true)
    public CourtResponse getCourtById(Long id) {
        Court court = getActiveCourtById(id);
        return CourtMapper.toResponse(court);
    }

    public CourtResponse createCourt(CourtRequest courtRequest) {
        validateCourtNumberIsUnique(courtRequest.courtNumber());

        SurfaceType surfaceType = getActiveSurfaceTypeById(courtRequest.surfaceTypeId());

        Court court = new Court();
        court.setCourtNumber(courtRequest.courtNumber());
        court.setSurfaceType(surfaceType);

        Court savedCourt = courtDao.save(court);
        return CourtMapper.toResponse(savedCourt);
    }

    public CourtResponse updateCourt(Long id, CourtRequest courtRequest) {
        Court court = getActiveCourtById(id);

        if (!court.getCourtNumber().equals(courtRequest.courtNumber())) {
            validateCourtNumberIsUnique(courtRequest.courtNumber());
        }

        SurfaceType surfaceType = getActiveSurfaceTypeById(courtRequest.surfaceTypeId());

        court.setCourtNumber(courtRequest.courtNumber());
        court.setSurfaceType(surfaceType);

        Court savedCourt = courtDao.save(court);
        return CourtMapper.toResponse(savedCourt);
    }

    public void deleteCourt(Long id) {
        courtDao.softDelete(getActiveCourtById(id));
    }

    private Court getActiveCourtById(Long id) {
        return courtDao.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Court not found"));
    }

    private SurfaceType getActiveSurfaceTypeById(Long surfaceTypeId) {
        return surfaceTypeDao.findActiveById(surfaceTypeId)
                .orElseThrow(() -> new NotFoundException("Surface type not found"));
    }

    private void validateCourtNumberIsUnique(String courtNumber) {
        courtDao.findActiveByCourtNumber(courtNumber)
                .ifPresent(court -> {
                    throw new BadRequestException("Court number already exists");
                });
    }
}
