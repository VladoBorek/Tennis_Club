package com.example.tennis_club.daos;

import com.example.tennis_club.entities.Court;
import com.example.tennis_club.entities.SurfaceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourtDaoTest extends DaoUtils {

    @Autowired
    private CourtDao courtDao;

    @Test
    void saveShouldPersistNewCourtAndAssignId() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));

        Court court = new Court();
        court.setCourtNumber("COURT-1");
        court.setSurfaceType(surfaceType);

        Court savedCourt = courtDao.save(court);

        flushAndClear();

        assertThat(savedCourt.getId()).isNotNull();

        Court persistedCourt = entityManager.find(Court.class, savedCourt.getId());
        assertThat(persistedCourt).isNotNull();
        assertThat(persistedCourt.getCourtNumber()).isEqualTo("COURT-1");
        assertThat(persistedCourt.getSurfaceType().getId()).isEqualTo(surfaceType.getId());
        assertThat(persistedCourt.isDeleted()).isFalse();
    }

    @Test
    void saveShouldMergeDetachedExistingCourt() {
        SurfaceType clay = createSurfaceType("Clay", new BigDecimal("0.30"));
        SurfaceType grass = createSurfaceType("Grass", new BigDecimal("0.45"));
        Court court = createCourt("COURT-1", clay);
        flushAndClear();

        Court detachedCourt = entityManager.find(Court.class, court.getId());
        entityManager.detach(detachedCourt);

        SurfaceType managedGrass = entityManager.find(SurfaceType.class, grass.getId());
        detachedCourt.setCourtNumber("COURT-2");
        detachedCourt.setSurfaceType(managedGrass);

        Court updatedCourt = courtDao.save(detachedCourt);

        flushAndClear();

        Court persistedCourt = entityManager.find(Court.class, updatedCourt.getId());
        assertThat(persistedCourt.getCourtNumber()).isEqualTo("COURT-2");
        assertThat(persistedCourt.getSurfaceType().getId()).isEqualTo(grass.getId());
    }

    @Test
    void findByIdShouldReturnActiveCourt() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        flushAndClear();

        Optional<Court> result = courtDao.findById(court.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getCourtNumber()).isEqualTo("COURT-1");
    }

    @Test
    void findByIdShouldReturnSoftDeletedCourt() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        court.setDeleted(true);
        flushAndClear();

        Optional<Court> result = courtDao.findById(court.getId());

        assertThat(result).isPresent();
        assertThat(result.get().isDeleted()).isTrue();
    }

    @Test
    void findActiveByIdShouldReturnActiveCourt() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        flushAndClear();

        Optional<Court> result = courtDao.findActiveById(court.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getCourtNumber()).isEqualTo("COURT-1");
    }

    @Test
    void findActiveByIdShouldReturnEmptyForSoftDeletedCourt() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        court.setDeleted(true);
        flushAndClear();

        Optional<Court> result = courtDao.findActiveById(court.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByIdShouldReturnEmptyForUnknownId() {
        Optional<Court> result = courtDao.findActiveById(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllActiveShouldReturnOnlyActiveCourts() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court1 = createCourt("COURT-1", surfaceType);
        Court court2 = createCourt("COURT-2", surfaceType);
        Court deletedCourt = createCourt("COURT-3", surfaceType);
        deletedCourt.setDeleted(true);
        flushAndClear();

        List<Court> result = courtDao.findAllActive();

        assertThat(result)
                .extracting(Court::getId)
                .containsExactlyInAnyOrder(court1.getId(), court2.getId())
                .doesNotContain(deletedCourt.getId());
    }

    @Test
    void findActiveByCourtNumberShouldReturnCourtWithSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        flushAndClear();

        Optional<Court> result = courtDao.findActiveByCourtNumber("COURT-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(court.getId());
        assertThat(result.get().getSurfaceType().getName()).isEqualTo("Clay");
    }

    @Test
    void findActiveByCourtNumberShouldReturnEmptyForUnknownCourtNumber() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        createCourt("COURT-1", surfaceType);
        flushAndClear();

        Optional<Court> result = courtDao.findActiveByCourtNumber("COURT-999");

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByCourtNumberShouldReturnEmptyForSoftDeletedCourt() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        court.setDeleted(true);
        flushAndClear();

        Optional<Court> result = courtDao.findActiveByCourtNumber("COURT-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByCourtNumberShouldReturnEmptyWhenSurfaceTypeIsSoftDeleted() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        createCourt("COURT-1", surfaceType);
        surfaceType.setDeleted(true);
        flushAndClear();

        Optional<Court> result = courtDao.findActiveByCourtNumber("COURT-1");

        assertThat(result).isEmpty();
    }

    @Test
    void existsActiveBySurfaceTypeIdShouldReturnTrueWhenActiveCourtUsesSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        createCourt("COURT-1", surfaceType);
        flushAndClear();

        boolean result = courtDao.existsActiveBySurfaceTypeId(surfaceType.getId());

        assertThat(result).isTrue();
    }

    @Test
    void existsActiveBySurfaceTypeIdShouldReturnFalseWhenNoCourtUsesSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        boolean result = courtDao.existsActiveBySurfaceTypeId(surfaceType.getId());

        assertThat(result).isFalse();
    }

    @Test
    void existsActiveBySurfaceTypeIdShouldReturnFalseWhenOnlySoftDeletedCourtUsesSurfaceType() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        court.setDeleted(true);
        flushAndClear();

        boolean result = courtDao.existsActiveBySurfaceTypeId(surfaceType.getId());

        assertThat(result).isFalse();
    }

    @Test
    void softDeleteShouldMarkCourtAsDeletedAndExcludeItFromActiveQueries() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        flushAndClear();

        Court existingCourt = entityManager.find(Court.class, court.getId());
        courtDao.softDelete(existingCourt);

        flushAndClear();

        assertThat(courtDao.findById(court.getId())).isPresent();
        assertThat(courtDao.findActiveById(court.getId())).isEmpty();
        assertThat(courtDao.findActiveByCourtNumber("COURT-1")).isEmpty();
        assertThat(courtDao.findAllActive()).isEmpty();
    }

    @Test
    void saveShouldRejectDuplicateCourtNumber() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        createCourt("COURT-1", surfaceType);
        flushAndClear();

        SurfaceType managedSurfaceType = entityManager.find(SurfaceType.class, surfaceType.getId());

        Court duplicate = new Court();
        duplicate.setCourtNumber("COURT-1");
        duplicate.setSurfaceType(managedSurfaceType);

        assertThrows(DataIntegrityViolationException.class, () -> courtDao.save(duplicate));
    }

    @Test
    void saveShouldRejectNullCourtNumber() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        flushAndClear();

        SurfaceType managedSurfaceType = entityManager.find(SurfaceType.class, surfaceType.getId());

        Court court = new Court();
        court.setSurfaceType(managedSurfaceType);

        assertThrows(DataIntegrityViolationException.class, () -> courtDao.save(court));
    }

    @Test
    void saveShouldRejectNullSurfaceType() {
        Court court = new Court();
        court.setCourtNumber("COURT-1");

        assertThrows(DataIntegrityViolationException.class, () -> courtDao.save(court));
    }
}