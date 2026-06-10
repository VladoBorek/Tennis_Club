package com.example.tennis_club.controllers;

import com.example.tennis_club.dtos.reservation.ReservationRequest;
import com.example.tennis_club.dtos.reservation.ReservationResponse;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import com.example.tennis_club.facades.ReservationFacade;
import com.example.tennis_club.models.GameType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import(GlobalExceptionHandler.class)
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ReservationFacade reservationFacade;

    @Test
    void getReservationByIdShouldReturnReservation() throws Exception {
        when(reservationFacade.getReservationById(1L)).thenReturn(reservationResponse());

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.courtNumber").value("COURT-1"))
                .andExpect(jsonPath("$.customerPhoneNumber").value("+421900000000"))
                .andExpect(jsonPath("$.gameType").value("SINGLES"));
    }

    @Test
    void getReservationByIdShouldReturnNotFoundWhenReservationDoesNotExist() throws Exception {
        when(reservationFacade.getReservationById(1L))
                .thenThrow(new NotFoundException("Reservation not found"));

        mockMvc.perform(get("/api/reservations/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Reservation not found"));
    }

    @Test
    void getReservationsByCourtNumberShouldReturnReservations() throws Exception {
        when(reservationFacade.getReservationsByCourtNumber("COURT-1"))
                .thenReturn(List.of(reservationResponse()));

        mockMvc.perform(get("/api/reservations/court/COURT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courtNumber").value("COURT-1"));

        verify(reservationFacade).getReservationsByCourtNumber("COURT-1");
    }

    @Test
    void getReservationsByCustomerPhoneNumberShouldPassFutureOnlyFlag() throws Exception {
        when(reservationFacade.getReservationsByCustomerPhoneNumber("+421900000000", true))
                .thenReturn(List.of(reservationResponse()));

        mockMvc.perform(get("/api/reservations/customer/+421900000000")
                        .param("futureOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerPhoneNumber").value("+421900000000"));

        verify(reservationFacade).getReservationsByCustomerPhoneNumber("+421900000000", true);
    }

    @Test
    void createReservationShouldReturnCreatedReservation() throws Exception {
        ReservationRequest request = reservationRequest();

        when(reservationFacade.createReservation(request)).thenReturn(reservationResponse());

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.price").value(18.00));

        verify(reservationFacade).createReservation(request);
    }

    @Test
    void createReservationShouldReturnBadRequestForInvalidRequestBody() throws Exception {
        ReservationRequest request = new ReservationRequest(
                "",
                "+421900000000",
                "John Doe",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 11, 0),
                GameType.SINGLES
        );

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors.courtNumber").exists());

        verifyNoInteractions(reservationFacade);
    }

    @Test
    void createReservationShouldReturnBadRequestWhenReservationOverlaps() throws Exception {
        ReservationRequest request = reservationRequest();

        when(reservationFacade.createReservation(request))
                .thenThrow(new BadRequestException("Reservation overlaps with an existing reservation"));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reservation overlaps with an existing reservation"));
    }

    @Test
    void createReservationShouldReturnBadRequestForInvalidJson() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid-json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request"));

        verifyNoInteractions(reservationFacade);
    }

    @Test
    void updateReservationShouldReturnUpdatedReservation() throws Exception {
        ReservationRequest request = reservationRequest();

        when(reservationFacade.updateReservation(1L, request)).thenReturn(reservationResponse());

        mockMvc.perform(put("/api/reservations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.courtNumber").value("COURT-1"));

        verify(reservationFacade).updateReservation(1L, request);
    }

    @Test
    void deleteReservationShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/reservations/1"))
                .andExpect(status().isNoContent());

        verify(reservationFacade).deleteReservation(1L);
    }

    private ReservationRequest reservationRequest() {
        return new ReservationRequest(
                "COURT-1",
                "+421900000000",
                "John Doe",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 11, 0),
                GameType.SINGLES
        );
    }

    private ReservationResponse reservationResponse() {
        return new ReservationResponse(
                1L,
                "COURT-1",
                10L,
                "Clay",
                "+421900000000",
                "John Doe",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 11, 0),
                GameType.SINGLES,
                new BigDecimal("18.00"),
                LocalDateTime.of(2026, 6, 1, 9, 0)
        );
    }
}