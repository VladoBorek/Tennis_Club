package com.example.tennis_club.services;

import com.example.tennis_club.daos.CourtDao;
import com.example.tennis_club.daos.SurfaceTypeDao;
import com.example.tennis_club.dtos.court.CourtRequest;
import com.example.tennis_club.dtos.court.CourtResponse;
import com.example.tennis_club.entities.Court;
import com.example.tennis_club.entities.SurfaceType;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

    @Mock
    private CourtDao courtDao;

    @Mock
    private SurfaceTypeDao surfaceTypeDao;

    @InjectMocks
    private CourtService courtService;

    @Test
    void getAllCourtsShouldReturnMappedActiveCourts() {
        SurfaceType clay = surfaceType("Clay", "0.30");
        SurfaceType grass = surfaceType("Grass", "0.45");
        Court court1 = court("COURT-1", clay);
        Court court2 = court("COURT-2", grass);

        when(courtDao.findAllActive()).thenReturn(List.of(court1, court2));

        List<CourtResponse> result = courtService.getAllCourts();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(CourtResponse::courtNumber)
                .containsExactly("COURT-1", "COURT-2");
        assertThat(result)
                .extracting(CourtResponse::surfaceTypeName)
                .containsExactly("Clay", "Grass");
    }

    @Test
    void getCourtByIdShouldReturnMappedCourtWhenFound() {
        Court court = court("COURT-1", surfaceType("Clay", "0.30"));
        when(courtDao.findActiveById(1L)).thenReturn(Optional.of(court));

        CourtResponse result = courtService.getCourtById(1L);

        assertThat(result.courtNumber()).isEqualTo("COURT-1");
        assertThat(result.surfaceTypeName()).isEqualTo("Clay");
        assertThat(result.pricePerMinute()).isEqualByComparingTo("0.30");
    }

    @Test
    void getCourtByIdShouldThrowWhenCourtNotFound() {
        when(courtDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.getCourtById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Court not found");
    }

    @Test
    void getActiveCourtByCourtNumberShouldReturnCourtWhenFound() {
        Court court = court("COURT-1", surfaceType("Clay", "0.30"));
        when(courtDao.findActiveByCourtNumber("COURT-1")).thenReturn(Optional.of(court));

        Court result = courtService.getActiveCourtByCourtNumber("COURT-1");

        assertThat(result).isSameAs(court);
    }

    @Test
    void getActiveCourtByCourtNumberShouldThrowWhenCourtNotFound() {
        when(courtDao.findActiveByCourtNumber("COURT-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.getActiveCourtByCourtNumber("COURT-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Court not found");
    }

    @Test
    void createCourtShouldSaveCourtWhenCourtNumberIsUniqueAndSurfaceTypeExists() {
        CourtRequest request = new CourtRequest("COURT-1", 1L);
        SurfaceType surfaceType = surfaceType("Clay", "0.30");
        Court savedCourt = court("COURT-1", surfaceType);

        when(courtDao.findActiveByCourtNumber("COURT-1")).thenReturn(Optional.empty());
        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.of(surfaceType));
        when(courtDao.save(any(Court.class))).thenReturn(savedCourt);

        CourtResponse result = courtService.createCourt(request);

        assertThat(result.courtNumber()).isEqualTo("COURT-1");
        assertThat(result.surfaceTypeName()).isEqualTo("Clay");

        verify(courtDao).save(argThat(court ->
                court.getCourtNumber().equals("COURT-1")
                        && court.getSurfaceType() == surfaceType
                        && !court.isDeleted()
        ));
    }

    @Test
    void createCourtShouldThrowWhenCourtNumberAlreadyExists() {
        CourtRequest request = new CourtRequest("COURT-1", 1L);
        Court existingCourt = court("COURT-1", surfaceType("Clay", "0.30"));

        when(courtDao.findActiveByCourtNumber("COURT-1")).thenReturn(Optional.of(existingCourt));

        assertThatThrownBy(() -> courtService.createCourt(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Court number already exists");

        verify(surfaceTypeDao, never()).findActiveById(any());
        verify(courtDao, never()).save(any());
    }

    @Test
    void createCourtShouldThrowWhenSurfaceTypeNotFound() {
        CourtRequest request = new CourtRequest("COURT-1", 1L);

        when(courtDao.findActiveByCourtNumber("COURT-1")).thenReturn(Optional.empty());
        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.createCourt(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Surface type not found");

        verify(courtDao, never()).save(any());
    }

    @Test
    void updateCourtShouldUpdateSurfaceTypeWhenCourtNumberIsUnchanged() {
        SurfaceType oldSurfaceType = surfaceType("Clay", "0.30");
        SurfaceType newSurfaceType = surfaceType("Grass", "0.45");
        Court existingCourt = court("COURT-1", oldSurfaceType);
        CourtRequest request = new CourtRequest("COURT-1", 2L);

        when(courtDao.findActiveById(1L)).thenReturn(Optional.of(existingCourt));
        when(surfaceTypeDao.findActiveById(2L)).thenReturn(Optional.of(newSurfaceType));
        when(courtDao.save(existingCourt)).thenReturn(existingCourt);

        CourtResponse result = courtService.updateCourt(1L, request);

        assertThat(result.courtNumber()).isEqualTo("COURT-1");
        assertThat(result.surfaceTypeName()).isEqualTo("Grass");

        verify(courtDao, never()).findActiveByCourtNumber(any());
        verify(courtDao).save(existingCourt);
    }

    @Test
    void updateCourtShouldUpdateCourtNumberWhenNewCourtNumberIsUnique() {
        SurfaceType surfaceType = surfaceType("Clay", "0.30");
        Court existingCourt = court("COURT-1", surfaceType);
        CourtRequest request = new CourtRequest("COURT-2", 1L);

        when(courtDao.findActiveById(1L)).thenReturn(Optional.of(existingCourt));
        when(courtDao.findActiveByCourtNumber("COURT-2")).thenReturn(Optional.empty());
        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.of(surfaceType));
        when(courtDao.save(existingCourt)).thenReturn(existingCourt);

        CourtResponse result = courtService.updateCourt(1L, request);

        assertThat(result.courtNumber()).isEqualTo("COURT-2");
        assertThat(result.surfaceTypeName()).isEqualTo("Clay");

        verify(courtDao).findActiveByCourtNumber("COURT-2");
        verify(courtDao).save(existingCourt);
    }

    @Test
    void updateCourtShouldThrowWhenCourtNotFound() {
        CourtRequest request = new CourtRequest("COURT-1", 1L);
        when(courtDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.updateCourt(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Court not found");

        verify(courtDao, never()).save(any());
    }

    @Test
    void updateCourtShouldThrowWhenNewCourtNumberAlreadyExists() {
        SurfaceType surfaceType = surfaceType("Clay", "0.30");
        Court existingCourt = court("COURT-1", surfaceType);
        Court conflictingCourt = court("COURT-2", surfaceType);
        CourtRequest request = new CourtRequest("COURT-2", 1L);

        when(courtDao.findActiveById(1L)).thenReturn(Optional.of(existingCourt));
        when(courtDao.findActiveByCourtNumber("COURT-2")).thenReturn(Optional.of(conflictingCourt));

        assertThatThrownBy(() -> courtService.updateCourt(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Court number already exists");

        verify(surfaceTypeDao, never()).findActiveById(any());
        verify(courtDao, never()).save(any());
    }

    @Test
    void updateCourtShouldThrowWhenSurfaceTypeNotFound() {
        SurfaceType surfaceType = surfaceType("Clay", "0.30");
        Court existingCourt = court("COURT-1", surfaceType);
        CourtRequest request = new CourtRequest("COURT-1", 2L);

        when(courtDao.findActiveById(1L)).thenReturn(Optional.of(existingCourt));
        when(surfaceTypeDao.findActiveById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.updateCourt(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Surface type not found");

        verify(courtDao, never()).save(any());
    }

    @Test
    void deleteCourtShouldSoftDeleteCourtWhenFound() {
        Court court = court("COURT-1", surfaceType("Clay", "0.30"));
        when(courtDao.findActiveById(1L)).thenReturn(Optional.of(court));

        courtService.deleteCourt(1L);

        verify(courtDao).softDelete(court);
    }

    @Test
    void deleteCourtShouldThrowWhenCourtNotFound() {
        when(courtDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courtService.deleteCourt(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Court not found");

        verify(courtDao, never()).softDelete(any());
    }

    private Court court(String courtNumber, SurfaceType surfaceType) {
        Court court = new Court();
        court.setCourtNumber(courtNumber);
        court.setSurfaceType(surfaceType);
        return court;
    }

    private SurfaceType surfaceType(String name, String pricePerMinute) {
        SurfaceType surfaceType = new SurfaceType();
        surfaceType.setName(name);
        surfaceType.setPricePerMinute(new BigDecimal(pricePerMinute));
        return surfaceType;
    }
}