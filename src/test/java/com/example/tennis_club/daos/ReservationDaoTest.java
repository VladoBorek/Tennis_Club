package com.example.tennis_club.daos;

import com.example.tennis_club.entities.Court;
import com.example.tennis_club.entities.Customer;
import com.example.tennis_club.entities.Reservation;
import com.example.tennis_club.entities.SurfaceType;
import com.example.tennis_club.models.GameType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationDaoTest extends DaoUtils {

    @Autowired
    private ReservationDao reservationDao;

    @Test
    void saveShouldPersistNewReservationAndAssignId() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setStartTime(LocalDateTime.of(2026, 1, 10, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 1, 10, 11, 0));
        reservation.setGameType(GameType.SINGLES);
        reservation.setPrice(new BigDecimal("18.00"));
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

        Reservation savedReservation = reservationDao.save(reservation);

        flushAndClear();

        assertThat(savedReservation.getId()).isNotNull();

        Reservation persistedReservation = entityManager.find(Reservation.class, savedReservation.getId());
        assertThat(persistedReservation).isNotNull();
        assertThat(persistedReservation.getCourt().getId()).isEqualTo(court.getId());
        assertThat(persistedReservation.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(persistedReservation.getGameType()).isEqualTo(GameType.SINGLES);
        assertThat(persistedReservation.getPrice()).isEqualByComparingTo("18.00");
        assertThat(persistedReservation.isDeleted()).isFalse();
    }

    @Test
    void saveShouldMergeDetachedExistingReservation() {
        SurfaceType clay = createSurfaceType("Clay", new BigDecimal("0.30"));
        SurfaceType grass = createSurfaceType("Grass", new BigDecimal("0.45"));
        Court court1 = createCourt("COURT-1", clay);
        Court court2 = createCourt("COURT-2", grass);
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        Reservation reservation = createReservation(
                court1,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        flushAndClear();

        Reservation detachedReservation = entityManager.find(Reservation.class, reservation.getId());
        entityManager.detach(detachedReservation);

        Court managedCourt2 = entityManager.find(Court.class, court2.getId());
        detachedReservation.setCourt(managedCourt2);
        detachedReservation.setGameType(GameType.DOUBLES);
        detachedReservation.setPrice(new BigDecimal("27.00"));

        Reservation updatedReservation = reservationDao.save(detachedReservation);

        flushAndClear();

        Reservation persistedReservation = entityManager.find(Reservation.class, updatedReservation.getId());
        assertThat(persistedReservation.getCourt().getId()).isEqualTo(court2.getId());
        assertThat(persistedReservation.getGameType()).isEqualTo(GameType.DOUBLES);
        assertThat(persistedReservation.getPrice()).isEqualByComparingTo("27.00");
    }

    @Test
    void findByIdShouldReturnActiveReservation() {
        Reservation reservation = createBasicReservation();
        flushAndClear();

        Optional<Reservation> result = reservationDao.findById(reservation.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getGameType()).isEqualTo(GameType.SINGLES);
    }

    @Test
    void findByIdShouldReturnSoftDeletedReservation() {
        Reservation reservation = createBasicReservation();
        reservation.setDeleted(true);
        flushAndClear();

        Optional<Reservation> result = reservationDao.findById(reservation.getId());

        assertThat(result).isPresent();
        assertThat(result.get().isDeleted()).isTrue();
    }

    @Test
    void findActiveByIdShouldReturnActiveReservation() {
        Reservation reservation = createBasicReservation();
        flushAndClear();

        Optional<Reservation> result = reservationDao.findActiveById(reservation.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(reservation.getId());
    }

    @Test
    void findActiveByIdShouldReturnEmptyForSoftDeletedReservation() {
        Reservation reservation = createBasicReservation();
        reservation.setDeleted(true);
        flushAndClear();

        Optional<Reservation> result = reservationDao.findActiveById(reservation.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void findAllActiveShouldReturnOnlyActiveReservations() {
        Reservation reservation1 = createBasicReservation();
        Reservation reservation2 = createBasicReservation("COURT-2", "+421900333444");
        Reservation deletedReservation = createBasicReservation("COURT-3", "+421900555666");
        deletedReservation.setDeleted(true);
        flushAndClear();

        List<Reservation> result = reservationDao.findAllActive();

        assertThat(result)
                .extracting(Reservation::getId)
                .containsExactlyInAnyOrder(reservation1.getId(), reservation2.getId())
                .doesNotContain(deletedReservation.getId());
    }

    @Test
    void softDeleteShouldMarkReservationAsDeletedAndExcludeItFromActiveQueries() {
        Reservation reservation = createBasicReservation();
        flushAndClear();

        Reservation existingReservation = entityManager.find(Reservation.class, reservation.getId());
        reservationDao.softDelete(existingReservation);

        flushAndClear();

        assertThat(reservationDao.findById(reservation.getId())).isPresent();
        assertThat(reservationDao.findActiveById(reservation.getId())).isEmpty();
        assertThat(reservationDao.findAllActive()).isEmpty();
    }

    @Test
    void findActiveByCourtNumberOrderByCreatedAtShouldReturnOnlyReservationsForRequestedCourt() {
        Reservation court1Reservation = createBasicReservation("COURT-1", "+421900111222");
        createBasicReservation("COURT-2", "+421900333444");
        flushAndClear();

        List<Reservation> result = reservationDao.findActiveByCourtNumberOrderByCreatedAt("COURT-1");

        assertThat(result)
                .extracting(Reservation::getId)
                .containsExactly(court1Reservation.getId());
    }

    @Test
    void findActiveByCourtNumberOrderByCreatedAtShouldSortByCreatedAtAscending() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation newest = createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 13, 0),
                LocalDateTime.of(2026, 1, 10, 14, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 3, 9, 0)
        );
        Reservation oldest = createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        Reservation middle = createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 11, 30),
                LocalDateTime.of(2026, 1, 10, 12, 30),
                GameType.DOUBLES,
                new BigDecimal("27.00"),
                LocalDateTime.of(2026, 1, 2, 9, 0)
        );
        flushAndClear();

        List<Reservation> result = reservationDao.findActiveByCourtNumberOrderByCreatedAt("COURT-1");

        assertThat(result)
                .extracting(Reservation::getId)
                .containsExactly(oldest.getId(), middle.getId(), newest.getId());
    }

    @Test
    void findActiveByCourtNumberOrderByCreatedAtShouldExcludeSoftDeletedReservation() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        Customer activeCustomer = createCustomer("+421900111222", "Peter Novak");
        Customer deletedReservationCustomer = createCustomer("+421900333444", "Martin Hrasko");

        Reservation activeReservation = createReservation(
                court,
                activeCustomer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );

        Reservation deletedReservation = createReservation(
                court,
                deletedReservationCustomer,
                LocalDateTime.of(2026, 1, 10, 12, 0),
                LocalDateTime.of(2026, 1, 10, 13, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );
        deletedReservation.setDeleted(true);

        flushAndClear();

        List<Reservation> result = reservationDao.findActiveByCourtNumberOrderByCreatedAt("COURT-1");

        assertThat(result)
                .extracting(Reservation::getId)
                .containsExactly(activeReservation.getId());
    }

    @Test
    void findActiveByCourtNumberOrderByCreatedAtShouldExcludeReservationWhenCourtIsSoftDeleted() {
        Reservation reservation = createBasicReservation();
        reservation.getCourt().setDeleted(true);
        flushAndClear();

        List<Reservation> result = reservationDao.findActiveByCourtNumberOrderByCreatedAt("COURT-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByCourtNumberOrderByCreatedAtShouldExcludeReservationWhenCustomerIsSoftDeleted() {
        Reservation reservation = createBasicReservation();
        reservation.getCustomer().setDeleted(true);
        flushAndClear();

        List<Reservation> result = reservationDao.findActiveByCourtNumberOrderByCreatedAt("COURT-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByCourtNumberOrderByCreatedAtShouldIncludeReservationWhenSurfaceTypeIsSoftDeleted() {
        Reservation reservation = createBasicReservation();
        reservation.getCourt().getSurfaceType().setDeleted(true);
        flushAndClear();

        List<Reservation> result = reservationDao.findActiveByCourtNumberOrderByCreatedAt("COURT-1");

        assertThat(result)
                .extracting(Reservation::getId)
                .containsExactly(reservation.getId());
    }

    @Test
    void findActiveByCustomerPhoneNumberShouldReturnOnlyReservationsForRequestedPhoneNumber() {
        Reservation reservation = createBasicReservation("COURT-1", "+421900111222");
        createBasicReservation("COURT-2", "+421900333444");
        flushAndClear();

        List<Reservation> result = reservationDao.findActiveByCustomerPhoneNumber("+421900111222");

        assertThat(result)
                .extracting(Reservation::getId)
                .containsExactly(reservation.getId());
    }

    @Test
    void findActiveByCustomerPhoneNumberShouldSortByStartTimeAscending() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation latest = createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 14, 0),
                LocalDateTime.of(2026, 1, 10, 15, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        Reservation earliest = createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 5)
        );
        Reservation middle = createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 12, 0),
                LocalDateTime.of(2026, 1, 10, 13, 0),
                GameType.DOUBLES,
                new BigDecimal("27.00"),
                LocalDateTime.of(2026, 1, 1, 9, 10)
        );
        flushAndClear();

        List<Reservation> result = reservationDao.findActiveByCustomerPhoneNumber("+421900111222");

        assertThat(result)
                .extracting(Reservation::getId)
                .containsExactly(earliest.getId(), middle.getId(), latest.getId());
    }

    @Test
    void findFutureActiveByCustomerPhoneNumberShouldReturnOnlyReservationsStartingAfterNow() {
        SurfaceType surfaceType = createSurfaceType("Clay", new BigDecimal("0.30"));
        Court court = createCourt("COURT-1", surfaceType);
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        LocalDateTime now = LocalDateTime.of(2026, 1, 10, 12, 0);

        createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        createReservation(
                court,
                customer,
                now,
                LocalDateTime.of(2026, 1, 10, 13, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 5)
        );
        Reservation future = createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 14, 0),
                LocalDateTime.of(2026, 1, 10, 15, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 10)
        );
        flushAndClear();

        List<Reservation> result = reservationDao.findFutureActiveByCustomerPhoneNumber("+421900111222", now);

        assertThat(result)
                .extracting(Reservation::getId)
                .containsExactly(future.getId());
    }

    @Test
    void existsOverlappingActiveReservationShouldReturnTrueForPartialOverlapAtStart() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        flushAndClear();

        boolean result = reservationDao.existsOverlappingActiveReservation(
                court.getId(),
                LocalDateTime.of(2026, 1, 10, 9, 30),
                LocalDateTime.of(2026, 1, 10, 10, 30)
        );

        assertThat(result).isTrue();
    }

    @Test
    void existsOverlappingActiveReservationShouldReturnTrueForPartialOverlapAtEnd() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        flushAndClear();

        boolean result = reservationDao.existsOverlappingActiveReservation(
                court.getId(),
                LocalDateTime.of(2026, 1, 10, 10, 30),
                LocalDateTime.of(2026, 1, 10, 11, 30)
        );

        assertThat(result).isTrue();
    }

    @Test
    void existsOverlappingActiveReservationShouldReturnTrueWhenRequestedIntervalContainsExistingReservation() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        flushAndClear();

        boolean result = reservationDao.existsOverlappingActiveReservation(
                court.getId(),
                LocalDateTime.of(2026, 1, 10, 9, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)
        );

        assertThat(result).isTrue();
    }

    @Test
    void existsOverlappingActiveReservationShouldReturnTrueWhenRequestedIntervalIsInsideExistingReservation() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0),
                GameType.SINGLES,
                new BigDecimal("36.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        flushAndClear();

        boolean result = reservationDao.existsOverlappingActiveReservation(
                court.getId(),
                LocalDateTime.of(2026, 1, 10, 10, 30),
                LocalDateTime.of(2026, 1, 10, 11, 0)
        );

        assertThat(result).isTrue();
    }

    @Test
    void existsOverlappingActiveReservationShouldReturnFalseWhenRequestedIntervalEndsAtExistingStart() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        flushAndClear();

        boolean result = reservationDao.existsOverlappingActiveReservation(
                court.getId(),
                LocalDateTime.of(2026, 1, 10, 9, 0),
                LocalDateTime.of(2026, 1, 10, 10, 0)
        );

        assertThat(result).isFalse();
    }

    @Test
    void existsOverlappingActiveReservationShouldReturnFalseWhenRequestedIntervalStartsAtExistingEnd() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");
        createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
        flushAndClear();

        boolean result = reservationDao.existsOverlappingActiveReservation(
                court.getId(),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                LocalDateTime.of(2026, 1, 10, 12, 0)
        );

        assertThat(result).isFalse();
    }

    @Test
    void existsOverlappingActiveReservationShouldIgnoreSoftDeletedReservation() {
        Reservation reservation = createBasicReservation();
        reservation.setDeleted(true);
        flushAndClear();

        boolean result = reservationDao.existsOverlappingActiveReservation(
                reservation.getCourt().getId(),
                LocalDateTime.of(2026, 1, 10, 10, 30),
                LocalDateTime.of(2026, 1, 10, 11, 30)
        );

        assertThat(result).isFalse();
    }

    @Test
    void existsOverlappingActiveReservationShouldIgnoreReservationsOnOtherCourts() {
        Reservation otherCourtReservation = createBasicReservation("COURT-1", "+421900111222");
        Court requestedCourt = createCourtWithSurface("COURT-2");
        flushAndClear();

        boolean result = reservationDao.existsOverlappingActiveReservation(
                requestedCourt.getId(),
                otherCourtReservation.getStartTime(),
                otherCourtReservation.getEndTime()
        );

        assertThat(result).isFalse();
    }

    @Test
    void saveShouldRejectNullCourt() {
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation reservation = new Reservation();
        reservation.setCustomer(customer);
        reservation.setStartTime(LocalDateTime.of(2026, 1, 10, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 1, 10, 11, 0));
        reservation.setGameType(GameType.SINGLES);
        reservation.setPrice(new BigDecimal("18.00"));
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

        assertThrows(DataIntegrityViolationException.class, () -> reservationDao.save(reservation));
    }

    @Test
    void saveShouldRejectNullCustomer() {
        Court court = createCourtWithSurface("COURT-1");

        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setStartTime(LocalDateTime.of(2026, 1, 10, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 1, 10, 11, 0));
        reservation.setGameType(GameType.SINGLES);
        reservation.setPrice(new BigDecimal("18.00"));
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

        assertThrows(DataIntegrityViolationException.class, () -> reservationDao.save(reservation));
    }

    @Test
    void saveShouldRejectNullStartTime() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setEndTime(LocalDateTime.of(2026, 1, 10, 11, 0));
        reservation.setGameType(GameType.SINGLES);
        reservation.setPrice(new BigDecimal("18.00"));
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

        assertThrows(DataIntegrityViolationException.class, () -> reservationDao.save(reservation));
    }

    @Test
    void saveShouldRejectNullEndTime() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setStartTime(LocalDateTime.of(2026, 1, 10, 10, 0));
        reservation.setGameType(GameType.SINGLES);
        reservation.setPrice(new BigDecimal("18.00"));
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

        assertThrows(DataIntegrityViolationException.class, () -> reservationDao.save(reservation));
    }

    @Test
    void saveShouldRejectNullGameType() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setStartTime(LocalDateTime.of(2026, 1, 10, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 1, 10, 11, 0));
        reservation.setPrice(new BigDecimal("18.00"));
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

        assertThrows(DataIntegrityViolationException.class, () -> reservationDao.save(reservation));
    }

    @Test
    void saveShouldRejectNullPrice() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setStartTime(LocalDateTime.of(2026, 1, 10, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 1, 10, 11, 0));
        reservation.setGameType(GameType.SINGLES);
        reservation.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));

        assertThrows(DataIntegrityViolationException.class, () -> reservationDao.save(reservation));
    }

    @Test
    void saveShouldAutoFillCreatedAtWhenCreatedAtIsNull() {
        Court court = createCourtWithSurface("COURT-1");
        Customer customer = createCustomer("+421900111222", "Peter Novak");

        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setStartTime(LocalDateTime.of(2026, 1, 10, 10, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 1, 10, 11, 0));
        reservation.setGameType(GameType.SINGLES);
        reservation.setPrice(new BigDecimal("18.00"));

        Reservation savedReservation = reservationDao.save(reservation);

        flushAndClear();

        Reservation persistedReservation = entityManager.find(Reservation.class, savedReservation.getId());
        assertThat(persistedReservation.getCreatedAt()).isNotNull();
    }

    private Reservation createBasicReservation() {
        return createBasicReservation("COURT-1", "+421900111222");
    }

    private Reservation createBasicReservation(String courtNumber, String phoneNumber) {
        Court court = createCourtWithSurface(courtNumber);
        Customer customer = createCustomer(phoneNumber, "Peter Novak");

        return createReservation(
                court,
                customer,
                LocalDateTime.of(2026, 1, 10, 10, 0),
                LocalDateTime.of(2026, 1, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 1, 1, 9, 0)
        );
    }

    private Court createCourtWithSurface(String courtNumber) {
        SurfaceType surfaceType = createSurfaceType("Surface " + courtNumber, new BigDecimal("0.30"));
        return createCourt(courtNumber, surfaceType);
    }
}