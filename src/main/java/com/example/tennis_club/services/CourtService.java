package com.example.tennis_club.services;


import com.example.tennis_club.daos.CourtDao;
import com.example.tennis_club.dtos.court.CourtRequest;
import com.example.tennis_club.dtos.court.CourtResponse;
import com.example.tennis_club.mappers.CourtMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CourtService {

    private final CourtDao courtDao;

    public CourtService(CourtDao courtDao) {
        this.courtDao = courtDao;
    }

    public List<CourtResponse> getAllCourts() {
        return CourtMapper.toResponseList(courtDao.findAllActive());
    }

    public CourtResponse getCourtById(int id) {

    }

    public CourtResponse createCourt(CourtRequest courtRequest) {

    }

    public void updateCourt(CourtRequest courtRequest) {

    }

    public void deleteCourt(int id) {
    }
}
