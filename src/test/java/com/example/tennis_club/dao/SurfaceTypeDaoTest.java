package com.example.tennis_club.dao;

import com.example.tennis_club.entities.SurfaceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SurfaceTypeDaoTest extends DaoUtils {
    @Autowired
    private SurfaceTypeDao surfaceTypeDao;

    @Test
    void saveShouldPersistNewSurfaceTypeAndAssignId() {
        SurfaceType surfaceType = new SurfaceType();
        surfaceType.setName("Clay");
        surfaceType.setPricePerMinute(new BigDecimal("0.30"));

        SurfaceType savedSurfaceType = surfaceTypeDao.save(surfaceType);

        flushAndClear();

        assertThat(savedSurfaceType.getId()).isNotNull();

        SurfaceType persistedSurfaceType = entityManager.find(SurfaceType.class, savedSurfaceType.getId());
        assertThat(persistedSurfaceType).isNotNull();
        assertThat(persistedSurfaceType.getName()).isEqualTo("Clay");
        assertThat(persistedSurfaceType.getPricePerMinute()).isEqualByComparingTo("0.30");
        assertThat(persistedSurfaceType.isDeleted()).isFalse();
    }

    @Test
    void saveShouldMergeExistingSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        SurfaceType existingSurfaceType = entityManager.find(SurfaceType.class, surfaceType.getId());
        existingSurfaceType.setName("Updated Clay");
        existingSurfaceType.setPricePerMinute(new BigDecimal("0.35"));

        SurfaceType updatedSurfaceType = surfaceTypeDao.save(existingSurfaceType);

        flushAndClear();

        SurfaceType persistedSurfaceType = entityManager.find(SurfaceType.class, updatedSurfaceType.getId());
        assertThat(persistedSurfaceType.getName()).isEqualTo("Updated Clay");
        assertThat(persistedSurfaceType.getPricePerMinute()).isEqualByComparingTo("0.35");
    }

    @Test
    void findByIdShouldReturnActiveSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        Optional<SurfaceType> result = surfaceTypeDao.findById(surfaceType.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Clay");
    }

    @Test
    void findByIdShouldReturnSoftDeletedSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        surfaceType.setDeleted(true);
        flushAndClear();

        Optional<SurfaceType> result = surfaceTypeDao.findById(surfaceType.getId());

        assertThat(result).isPresent();
        assertThat(result.get().isDeleted()).isTrue();
    }

    @Test
    void findActiveByIdShouldReturnActiveSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        Optional<SurfaceType> result = surfaceTypeDao.findActiveById(surfaceType.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Clay");
    }

    @Test
    void findActiveByIdShouldReturnEmptyForSoftDeletedSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        surfaceType.setDeleted(true);
        flushAndClear();

        Optional<SurfaceType> result = surfaceTypeDao.findActiveById(surfaceType.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByIdShouldReturnEmptyForUnknownId() {
        Optional<SurfaceType> result = surfaceTypeDao.findActiveById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllActiveShouldReturnOnlyActiveSurfaceTypes() {
        SurfaceType clay = createSurfaceType("Clay", new BigDecimal("0.30"));
        SurfaceType grass = createSurfaceType("Grass", new BigDecimal("0.45"));
        SurfaceType hard = createSurfaceType("Hard", new BigDecimal("0.40"));
        hard.setDeleted(true);
        flushAndClear();

        java.util.List<SurfaceType> result = surfaceTypeDao.findAllActive();

        assertThat(result)
                .extracting(SurfaceType::getId)
                .containsExactlyInAnyOrder(clay.getId(), grass.getId())
                .doesNotContain(hard.getId());
    }

    @Test
    void findActiveByNameShouldReturnSurfaceTypeIgnoringCase() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        Optional<SurfaceType> result = surfaceTypeDao.findActiveByName("cLaY");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(surfaceType.getId());
    }

    @Test
    void findActiveByNameShouldReturnEmptyForUnknownName() {
        createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        Optional<SurfaceType> result = surfaceTypeDao.findActiveByName("Grass");

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByNameShouldReturnEmptyForSoftDeletedSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        surfaceType.setDeleted(true);
        flushAndClear();

        Optional<SurfaceType> result = surfaceTypeDao.findActiveByName("Clay");

        assertThat(result).isEmpty();
    }

    @Test
    void softDeleteShouldMarkSurfaceTypeAsDeletedAndExcludeItFromActiveQueries() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        SurfaceType existingSurfaceType = entityManager.find(SurfaceType.class, surfaceType.getId());
        surfaceTypeDao.softDelete(existingSurfaceType);

        flushAndClear();

        assertThat(surfaceTypeDao.findById(surfaceType.getId())).isPresent();
        assertThat(surfaceTypeDao.findActiveById(surfaceType.getId())).isEmpty();
        assertThat(surfaceTypeDao.findActiveByName("Clay")).isEmpty();
        assertThat(surfaceTypeDao.findAllActive()).isEmpty();
    }

    @Test
    void saveShouldRejectDuplicateSurfaceTypeName() {
        createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        SurfaceType duplicate = new SurfaceType();
        duplicate.setName("Clay");
        duplicate.setPricePerMinute(new BigDecimal("0.35"));

        assertThrows(DataIntegrityViolationException.class, () -> surfaceTypeDao.save(duplicate));
    }

    @Test
    void saveShouldRejectNullName() {
        SurfaceType surfaceType = new SurfaceType();
        surfaceType.setPricePerMinute(new BigDecimal("0.30"));

        assertThrows(DataIntegrityViolationException.class, () -> surfaceTypeDao.save(surfaceType));
    }

    @Test
    void saveShouldRejectNullPricePerMinute() {
        SurfaceType surfaceType = new SurfaceType();
        surfaceType.setName("Clay");

        assertThrows(DataIntegrityViolationException.class, () -> surfaceTypeDao.save(surfaceType));
    }
}
