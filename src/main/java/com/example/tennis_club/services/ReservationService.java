package com.example.tennis_club.services;

import com.example.tennis_club.daos.ReservationDao;
import com.example.tennis_club.entities.Court;
import com.example.tennis_club.entities.Reservation;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import com.example.tennis_club.mappers.ReservationMapper;
import com.example.tennis_club.models.GameType;
import com.example.tennis_club.models.ReservationCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationService {
    private static final BigDecimal DOUBLES_MULTIPLIER = new BigDecimal("1.5");

    private final ReservationDao reservationDao;

    public ReservationService(ReservationDao reservationDao) {
        this.reservationDao = reservationDao;
    }

    @Transactional(readOnly = true)
    public Reservation getActiveReservationById(Long id) {
        return reservationDao.findActiveById(id)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
    }

    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByCourtNumber(String courtNumber) {
        return reservationDao.findActiveByCourtNumberOrderByCreatedAt(courtNumber);
    }

    @Transactional(readOnly = true)
    public List<Reservation> getReservationsByCustomerPhoneNumber(String phoneNumber, boolean futureOnly) {
        if (futureOnly) {
            return reservationDao.findFutureActiveByCustomerPhoneNumber(phoneNumber, LocalDateTime.now());
        }

        return reservationDao.findActiveByCustomerPhoneNumber(phoneNumber);
    }

    public Reservation createReservation(ReservationCommand command) {
        validateTimeInterval(command.startTime(), command.endTime());
        validateNoOverlap(command.court().getId(), command.startTime(), command.endTime());

        BigDecimal price = calculateReservationPrice(
                command.court(),
                command.startTime(),
                command.endTime(),
                command.gameType()
        );

        Reservation reservation = ReservationMapper.toEntity(command, price);
        return reservationDao.save(reservation);
    }

    public Reservation updateReservation(Long id, ReservationCommand command) {
        Reservation reservation = getActiveReservationById(id);

        validateTimeInterval(command.startTime(), command.endTime());
        validateNoOverlapForUpdate(
                id,
                command.court().getId(),
                command.startTime(),
                command.endTime()
        );

        BigDecimal price = calculateReservationPrice(
                command.court(),
                command.startTime(),
                command.endTime(),
                command.gameType()
        );

        ReservationMapper.updateEntity(reservation, command, price);
        return reservationDao.save(reservation);
    }

    public void deleteReservation(Long id) {
        Reservation reservation = getActiveReservationById(id);
        reservationDao.softDelete(reservation);
    }

    private void validateTimeInterval(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Reservation start time must be before end time");
        }
    }

    private void validateNoOverlap(Long courtId, LocalDateTime startTime, LocalDateTime endTime) {
        if (reservationDao.existsOverlappingActiveReservation(courtId, startTime, endTime)) {
            throw new BadRequestException("Reservation overlaps with an existing reservation");
        }
    }

    private void validateNoOverlapForUpdate(
            Long reservationId,
            Long courtId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (reservationDao.existsOverlappingActiveReservationExcludingId(
                reservationId,
                courtId,
                startTime,
                endTime
        )) {
            throw new BadRequestException("Reservation overlaps with an existing reservation");
        }
    }

    private BigDecimal calculateReservationPrice(
            Court court,
            LocalDateTime startTime,
            LocalDateTime endTime,
            GameType gameType
    ) {
        long minutes = Duration.between(startTime, endTime).toMinutes();

        BigDecimal basePrice = court.getSurfaceType()
                .getPricePerMinute()
                .multiply(BigDecimal.valueOf(minutes));

        if (gameType == GameType.DOUBLES) {
            basePrice = basePrice.multiply(DOUBLES_MULTIPLIER);
        }

        return basePrice.setScale(2, RoundingMode.HALF_UP);
    }
}
