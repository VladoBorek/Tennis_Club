package com.example.tennis_club.mappers;

import com.example.tennis_club.dtos.reservation.ReservationResponse;
import com.example.tennis_club.entities.Reservation;
import com.example.tennis_club.models.ReservationCommand;

import java.math.BigDecimal;
import java.util.List;

public final class ReservationMapper {

    public static ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getCourt().getCourtNumber(),
                reservation.getCourt().getSurfaceType().getId(),
                reservation.getCourt().getSurfaceType().getName(),
                reservation.getCustomer().getPhoneNumber(),
                reservation.getCustomer().getName(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getGameType(),
                reservation.getPrice(),
                reservation.getCreatedAt()
        );
    }

    public static List<ReservationResponse> toResponseList(List<Reservation> reservations) {
        return reservations.stream()
                .map(ReservationMapper::toResponse)
                .toList();
    }

    public static Reservation toEntity(ReservationCommand command, BigDecimal price) {
        Reservation reservation = new Reservation();
        applyCommand(reservation, command, price);
        return reservation;
    }

    public static void updateEntity(Reservation reservation, ReservationCommand command, BigDecimal price) {
        applyCommand(reservation, command, price);
    }

    private static void applyCommand(Reservation reservation, ReservationCommand command, BigDecimal price) {
        reservation.setCourt(command.court());
        reservation.setCustomer(command.customer());
        reservation.setStartTime(command.startTime());
        reservation.setEndTime(command.endTime());
        reservation.setGameType(command.gameType());
        reservation.setPrice(price);
    }

}