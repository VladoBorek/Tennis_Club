package com.example.tennis_club.services;

import com.example.tennis_club.daos.CourtDao;
import com.example.tennis_club.daos.SurfaceTypeDao;
import com.example.tennis_club.dtos.surface.SurfaceTypeRequest;
import com.example.tennis_club.dtos.surface.SurfaceTypeResponse;
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
class SurfaceTypeServiceTest {

    @Mock
    private SurfaceTypeDao surfaceTypeDao;

    @Mock
    private CourtDao courtDao;

    @InjectMocks
    private SurfaceTypeService surfaceTypeService;

    @Test
    void getAllSurfaceTypesShouldReturnMappedActiveSurfaceTypes() {
        SurfaceType clay = surfaceType(1L, "Clay", "0.30");
        SurfaceType grass = surfaceType(2L, "Grass", "0.45");
        when(surfaceTypeDao.findAllActive()).thenReturn(List.of(clay, grass));

        List<SurfaceTypeResponse> result = surfaceTypeService.getAllSurfaceTypes();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(SurfaceTypeResponse::name)
                .containsExactly("Clay", "Grass");
        assertThat(result)
                .extracting(SurfaceTypeResponse::pricePerMinute)
                .containsExactly(new BigDecimal("0.30"), new BigDecimal("0.45"));
    }

    @Test
    void getSurfaceTypeByIdShouldReturnMappedSurfaceTypeWhenFound() {
        SurfaceType surfaceType = surfaceType(1L, "Clay", "0.30");
        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.of(surfaceType));

        SurfaceTypeResponse result = surfaceTypeService.getSurfaceTypeById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Clay");
        assertThat(result.pricePerMinute()).isEqualByComparingTo("0.30");
    }

    @Test
    void getSurfaceTypeByIdShouldThrowWhenNotFound() {
        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surfaceTypeService.getSurfaceTypeById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Surface type not found");
    }

    @Test
    void createSurfaceTypeShouldSaveNewSurfaceTypeWhenNameIsUnique() {
        SurfaceTypeRequest request = new SurfaceTypeRequest("Clay", new BigDecimal("0.30"));
        SurfaceType savedSurfaceType = surfaceType(1L, "Clay", "0.30");

        when(surfaceTypeDao.findActiveByName("Clay")).thenReturn(Optional.empty());
        when(surfaceTypeDao.save(any(SurfaceType.class))).thenReturn(savedSurfaceType);

        SurfaceTypeResponse result = surfaceTypeService.createSurfaceType(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Clay");
        assertThat(result.pricePerMinute()).isEqualByComparingTo("0.30");

        verify(surfaceTypeDao).save(argThat(surfaceType ->
                surfaceType.getName().equals("Clay")
                        && surfaceType.getPricePerMinute().compareTo(new BigDecimal("0.30")) == 0
                        && !surfaceType.isDeleted()
        ));
    }

    @Test
    void createSurfaceTypeShouldThrowWhenNameAlreadyExists() {
        SurfaceTypeRequest request = new SurfaceTypeRequest("Clay", new BigDecimal("0.30"));
        when(surfaceTypeDao.findActiveByName("Clay")).thenReturn(Optional.of(surfaceType(1L, "Clay", "0.30")));

        assertThatThrownBy(() -> surfaceTypeService.createSurfaceType(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Surface type name already exists");

        verify(surfaceTypeDao, never()).save(any());
    }

    @Test
    void updateSurfaceTypeShouldUpdateNameAndPriceWhenNameIsUnchanged() {
        SurfaceType existingSurfaceType = surfaceType(1L, "Clay", "0.30");
        SurfaceTypeRequest request = new SurfaceTypeRequest("Clay", new BigDecimal("0.35"));

        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.of(existingSurfaceType));
        when(surfaceTypeDao.save(existingSurfaceType)).thenReturn(existingSurfaceType);

        SurfaceTypeResponse result = surfaceTypeService.updateSurfaceType(1L, request);

        assertThat(result.name()).isEqualTo("Clay");
        assertThat(result.pricePerMinute()).isEqualByComparingTo("0.35");
        assertThat(existingSurfaceType.getPricePerMinute()).isEqualByComparingTo("0.35");

        verify(surfaceTypeDao, never()).findActiveByName(any());
        verify(surfaceTypeDao).save(existingSurfaceType);
    }

    @Test
    void updateSurfaceTypeShouldUpdateNameAndPriceWhenNewNameIsUnique() {
        SurfaceType existingSurfaceType = surfaceType(1L, "Clay", "0.30");
        SurfaceTypeRequest request = new SurfaceTypeRequest("Updated Clay", new BigDecimal("0.35"));

        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.of(existingSurfaceType));
        when(surfaceTypeDao.findActiveByName("Updated Clay")).thenReturn(Optional.empty());
        when(surfaceTypeDao.save(existingSurfaceType)).thenReturn(existingSurfaceType);

        SurfaceTypeResponse result = surfaceTypeService.updateSurfaceType(1L, request);

        assertThat(result.name()).isEqualTo("Updated Clay");
        assertThat(result.pricePerMinute()).isEqualByComparingTo("0.35");

        verify(surfaceTypeDao).findActiveByName("Updated Clay");
        verify(surfaceTypeDao).save(existingSurfaceType);
    }

    @Test
    void updateSurfaceTypeShouldThrowWhenSurfaceTypeNotFound() {
        SurfaceTypeRequest request = new SurfaceTypeRequest("Clay", new BigDecimal("0.30"));
        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surfaceTypeService.updateSurfaceType(1L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Surface type not found");

        verify(surfaceTypeDao, never()).save(any());
    }

    @Test
    void updateSurfaceTypeShouldThrowWhenNewNameAlreadyExists() {
        SurfaceType existingSurfaceType = surfaceType(1L, "Clay", "0.30");
        SurfaceType conflictingSurfaceType = surfaceType(2L, "Grass", "0.45");
        SurfaceTypeRequest request = new SurfaceTypeRequest("Grass", new BigDecimal("0.35"));

        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.of(existingSurfaceType));
        when(surfaceTypeDao.findActiveByName("Grass")).thenReturn(Optional.of(conflictingSurfaceType));

        assertThatThrownBy(() -> surfaceTypeService.updateSurfaceType(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Surface type name already exists");

        verify(surfaceTypeDao, never()).save(any());
    }

    @Test
    void deleteSurfaceTypeShouldSoftDeleteWhenNotUsedByActiveCourts() {
        SurfaceType surfaceType = surfaceType(1L, "Clay", "0.30");

        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.of(surfaceType));
        when(courtDao.existsActiveBySurfaceTypeId(1L)).thenReturn(false);

        surfaceTypeService.deleteSurfaceType(1L);

        verify(surfaceTypeDao).softDelete(surfaceType);
    }

    @Test
    void deleteSurfaceTypeShouldThrowWhenSurfaceTypeNotFound() {
        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> surfaceTypeService.deleteSurfaceType(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Surface type not found");

        verify(courtDao, never()).existsActiveBySurfaceTypeId(any());
        verify(surfaceTypeDao, never()).softDelete(any());
    }

    @Test
    void deleteSurfaceTypeShouldThrowWhenUsedByActiveCourt() {
        SurfaceType surfaceType = surfaceType(1L, "Clay", "0.30");

        when(surfaceTypeDao.findActiveById(1L)).thenReturn(Optional.of(surfaceType));
        when(courtDao.existsActiveBySurfaceTypeId(1L)).thenReturn(true);

        assertThatThrownBy(() -> surfaceTypeService.deleteSurfaceType(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Surface type is used by active courts");

        verify(surfaceTypeDao, never()).softDelete(any());
    }

    private SurfaceType surfaceType(Long id, String name, String pricePerMinute) {
        SurfaceType surfaceType = new SurfaceType();
        surfaceType.setId(id);
        surfaceType.setName(name);
        surfaceType.setPricePerMinute(new BigDecimal(pricePerMinute));
        return surfaceType;
    }
}