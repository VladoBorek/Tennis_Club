package com.example.tennis_club.controllers;

import com.example.tennis_club.dtos.reservation.ReservationRequest;
import com.example.tennis_club.dtos.reservation.ReservationResponse;
import com.example.tennis_club.facades.ReservationFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reservations", description = "Reservation management")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationFacade reservationFacade;

    public ReservationController(ReservationFacade reservationFacade) {
        this.reservationFacade = reservationFacade;
    }

    @Operation(summary = "Get reservation by ID")
    @ApiResponse(responseCode = "200", description = "Reservation returned")
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationFacade.getReservationById(id));
    }

    @Operation(summary = "Get reservations by court number")
    @ApiResponse(responseCode = "200", description = "Reservations returned, ordered by reservation creation date")
    @GetMapping("/court/{courtNumber}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByCourtNumber(@PathVariable String courtNumber) {
        return ResponseEntity.ok(reservationFacade.getReservationsByCourtNumber(courtNumber));
    }

    @Operation(summary = "Get reservations by customer phone number")
    @ApiResponse(responseCode = "200", description = "Reservations returned")
    @GetMapping("/customer/{phoneNumber}")
    public ResponseEntity<List<ReservationResponse>> getReservationsByCustomerPhoneNumber(
            @PathVariable String phoneNumber,
            @Parameter(description = "If true, only future reservations are returned")
            @RequestParam(defaultValue = "false") boolean futureOnly
    ) {
        return ResponseEntity.ok(
                reservationFacade.getReservationsByCustomerPhoneNumber(phoneNumber, futureOnly)
        );
    }

    @Operation(summary = "Create reservation")
    @ApiResponse(responseCode = "201", description = "Reservation created with calculated price")
    @ApiResponse(responseCode = "400", description = "Invalid request or overlapping reservation")
    @ApiResponse(responseCode = "404", description = "Court not found")
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(reservationFacade.createReservation(request));
    }

    @Operation(summary = "Update reservation")
    @ApiResponse(responseCode = "200", description = "Reservation updated")
    @ApiResponse(responseCode = "400", description = "Invalid request or overlapping reservation")
    @ApiResponse(responseCode = "404", description = "Reservation or court not found")
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody ReservationRequest request
    ) {
        return ResponseEntity.ok(reservationFacade.updateReservation(id, request));
    }

    @Operation(summary = "Delete reservation")
    @ApiResponse(responseCode = "204", description = "Reservation deleted")
    @ApiResponse(responseCode = "404", description = "Reservation not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationFacade.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}