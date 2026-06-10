package com.example.tennis_club.facades;

import com.example.tennis_club.dtos.reservation.ReservationRequest;
import com.example.tennis_club.dtos.reservation.ReservationResponse;
import com.example.tennis_club.entities.Court;
import com.example.tennis_club.entities.Customer;
import com.example.tennis_club.entities.Reservation;
import com.example.tennis_club.mappers.ReservationMapper;
import com.example.tennis_club.models.ReservationCommand;
import com.example.tennis_club.services.CourtService;
import com.example.tennis_club.services.CustomerService;
import com.example.tennis_club.services.ReservationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional
public class ReservationFacade {

    private final ReservationService reservationService;
    private final CourtService courtService;
    private final CustomerService customerService;

    public ReservationFacade(
            ReservationService reservationService,
            CourtService courtService,
            CustomerService customerService
    ) {
        this.reservationService = reservationService;
        this.courtService = courtService;
        this.customerService = customerService;
    }

    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        return ReservationMapper.toResponse(reservationService.getActiveReservationById(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByCourtNumber(String courtNumber) {
        return ReservationMapper.toResponseList(
                reservationService.getReservationsByCourtNumber(courtNumber)
        );
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByCustomerPhoneNumber(String phoneNumber, boolean futureOnly) {
        return ReservationMapper.toResponseList(
                reservationService.getReservationsByCustomerPhoneNumber(phoneNumber, futureOnly)
        );
    }

    public ReservationResponse createReservation(ReservationRequest request) {
        Court court = courtService.getActiveCourtByCourtNumber(request.courtNumber());
        Customer customer = customerService.resolveCustomer(
                request.customerPhoneNumber(),
                request.customerName()
        );

        ReservationCommand command = new ReservationCommand(
                court,
                customer,
                request.startTime(),
                request.endTime(),
                request.gameType()
        );

        Reservation reservation = reservationService.createReservation(command);

        return ReservationMapper.toResponse(reservation);
    }

    public ReservationResponse updateReservation(Long id, ReservationRequest request) {
        Court court = courtService.getActiveCourtByCourtNumber(request.courtNumber());
        Customer customer = customerService.resolveCustomer(
                request.customerPhoneNumber(),
                request.customerName()
        );

        ReservationCommand command = new ReservationCommand(
                court,
                customer,
                request.startTime(),
                request.endTime(),
                request.gameType()
        );

        Reservation reservation = reservationService.updateReservation(id, command);

        return ReservationMapper.toResponse(reservation);
    }

    public void deleteReservation(Long id) {
        reservationService.deleteReservation(id);
    }
}