package com.example.tennis_club.facades;

import com.example.tennis_club.dtos.reservation.ReservationRequest;
import com.example.tennis_club.dtos.reservation.ReservationResponse;
import com.example.tennis_club.entities.*;
import com.example.tennis_club.models.GameType;
import com.example.tennis_club.models.ReservationCommand;
import com.example.tennis_club.services.CourtService;
import com.example.tennis_club.services.CustomerService;
import com.example.tennis_club.services.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationFacadeTest {

    @Mock
    private ReservationService reservationService;

    @Mock
    private CourtService courtService;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private ReservationFacade reservationFacade;

    @Test
    void getReservationByIdShouldReturnMappedResponse() {
        Reservation reservation = reservation(
                1L,
                court(10L, "COURT-1", surfaceType(100L, "Clay", "0.30")),
                customer(20L, "+421900000000", "John Doe"),
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 11, 0),
                GameType.SINGLES,
                "18.00"
        );

        when(reservationService.getActiveReservationById(1L)).thenReturn(reservation);

        ReservationResponse result = reservationFacade.getReservationById(1L);

        assertReservationResponse(result, reservation);
    }

    @Test
    void getReservationsByCourtNumberShouldReturnMappedResponsesInServiceOrder() {
        Court court = court(10L, "COURT-1", surfaceType(100L, "Clay", "0.30"));

        Reservation firstReservation = reservation(
                1L,
                court,
                customer(20L, "+421900000001", "John Doe"),
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 11, 0),
                GameType.SINGLES,
                "18.00"
        );

        Reservation secondReservation = reservation(
                2L,
                court,
                customer(21L, "+421900000002", "Jane Doe"),
                LocalDateTime.of(2026, 6, 10, 12, 0),
                LocalDateTime.of(2026, 6, 10, 13, 0),
                GameType.DOUBLES,
                "27.00"
        );

        when(reservationService.getReservationsByCourtNumber("COURT-1"))
                .thenReturn(List.of(firstReservation, secondReservation));

        List<ReservationResponse> result = reservationFacade.getReservationsByCourtNumber("COURT-1");

        assertThat(result).hasSize(2);
        assertReservationResponse(result.get(0), firstReservation);
        assertReservationResponse(result.get(1), secondReservation);
    }

    @Test
    void getReservationsByCustomerPhoneNumberShouldPassFutureOnlyFlagAndReturnMappedResponses() {
        Reservation reservation = reservation(
                1L,
                court(10L, "COURT-1", surfaceType(100L, "Clay", "0.30")),
                customer(20L, "+421900000000", "John Doe"),
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 11, 0),
                GameType.SINGLES,
                "18.00"
        );

        when(reservationService.getReservationsByCustomerPhoneNumber("+421900000000", true))
                .thenReturn(List.of(reservation));

        List<ReservationResponse> result = reservationFacade.getReservationsByCustomerPhoneNumber("+421900000000", true);

        assertThat(result).hasSize(1);
        assertReservationResponse(result.get(0), reservation);
        verify(reservationService).getReservationsByCustomerPhoneNumber("+421900000000", true);
    }

    @Test
    void createReservationShouldResolveCourtAndCustomerCreateCommandAndReturnMappedResponse() {
        Court court = court(10L, "COURT-1", surfaceType(100L, "Clay", "0.30"));
        Customer customer = customer(20L, "+421900000000", "John Doe");

        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 11, 0);

        ReservationRequest request = new ReservationRequest(
                "COURT-1",
                "+421900000000",
                "John Doe",
                startTime,
                endTime,
                GameType.SINGLES
        );

        Reservation createdReservation = reservation(
                1L,
                court,
                customer,
                startTime,
                endTime,
                GameType.SINGLES,
                "18.00"
        );

        when(courtService.getActiveCourtByCourtNumber("COURT-1")).thenReturn(court);
        when(customerService.resolveCustomer("+421900000000", "John Doe")).thenReturn(customer);
        when(reservationService.createReservation(org.mockito.ArgumentMatchers.any(ReservationCommand.class)))
                .thenReturn(createdReservation);

        ReservationResponse result = reservationFacade.createReservation(request);

        ArgumentCaptor<ReservationCommand> captor = ArgumentCaptor.forClass(ReservationCommand.class);
        verify(reservationService).createReservation(captor.capture());

        ReservationCommand command = captor.getValue();
        assertThat(command.court()).isSameAs(court);
        assertThat(command.customer()).isSameAs(customer);
        assertThat(command.startTime()).isEqualTo(startTime);
        assertThat(command.endTime()).isEqualTo(endTime);
        assertThat(command.gameType()).isEqualTo(GameType.SINGLES);

        assertReservationResponse(result, createdReservation);
    }

    @Test
    void createReservationShouldStopWhenCourtLookupFails() {
        ReservationRequest request = new ReservationRequest(
                "COURT-1",
                "+421900000000",
                "John Doe",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 11, 0),
                GameType.SINGLES
        );

        RuntimeException exception = new RuntimeException("Court not found");

        when(courtService.getActiveCourtByCourtNumber("COURT-1")).thenThrow(exception);

        assertThatThrownBy(() -> reservationFacade.createReservation(request))
                .isSameAs(exception);

        verifyNoInteractions(customerService);
        verifyNoInteractions(reservationService);
    }

    @Test
    void updateReservationShouldResolveCourtAndCustomerUpdateCommandAndReturnMappedResponse() {
        Court court = court(10L, "COURT-1", surfaceType(100L, "Clay", "0.30"));
        Customer customer = customer(20L, "+421900000000", "John Doe");

        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 12, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 14, 0);

        ReservationRequest request = new ReservationRequest(
                "COURT-1",
                "+421900000000",
                "John Doe",
                startTime,
                endTime,
                GameType.DOUBLES
        );

        Reservation updatedReservation = reservation(
                1L,
                court,
                customer,
                startTime,
                endTime,
                GameType.DOUBLES,
                "54.00"
        );

        when(courtService.getActiveCourtByCourtNumber("COURT-1")).thenReturn(court);
        when(customerService.resolveCustomer("+421900000000", "John Doe")).thenReturn(customer);
        when(reservationService.updateReservation(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(ReservationCommand.class)))
                .thenReturn(updatedReservation);

        ReservationResponse result = reservationFacade.updateReservation(1L, request);

        ArgumentCaptor<ReservationCommand> captor = ArgumentCaptor.forClass(ReservationCommand.class);
        verify(reservationService).updateReservation(org.mockito.ArgumentMatchers.eq(1L), captor.capture());

        ReservationCommand command = captor.getValue();
        assertThat(command.court()).isSameAs(court);
        assertThat(command.customer()).isSameAs(customer);
        assertThat(command.startTime()).isEqualTo(startTime);
        assertThat(command.endTime()).isEqualTo(endTime);
        assertThat(command.gameType()).isEqualTo(GameType.DOUBLES);

        assertReservationResponse(result, updatedReservation);
    }

    @Test
    void updateReservationShouldStopWhenCustomerResolutionFails() {
        Court court = court(10L, "COURT-1", surfaceType(100L, "Clay", "0.30"));

        ReservationRequest request = new ReservationRequest(
                "COURT-1",
                "+421900000000",
                "Wrong Name",
                LocalDateTime.of(2026, 6, 10, 10, 0),
                LocalDateTime.of(2026, 6, 10, 11, 0),
                GameType.SINGLES
        );

        RuntimeException exception = new RuntimeException("Customer name does not match phone number");

        when(courtService.getActiveCourtByCourtNumber("COURT-1")).thenReturn(court);
        when(customerService.resolveCustomer("+421900000000", "Wrong Name")).thenThrow(exception);

        assertThatThrownBy(() -> reservationFacade.updateReservation(1L, request))
                .isSameAs(exception);

        verifyNoInteractions(reservationService);
    }

    @Test
    void deleteReservationShouldDelegateToReservationService() {
        reservationFacade.deleteReservation(1L);

        verify(reservationService).deleteReservation(1L);
    }

    private void assertReservationResponse(ReservationResponse response, Reservation reservation) {
        assertThat(response.id()).isEqualTo(reservation.getId());
        assertThat(response.courtNumber()).isEqualTo(reservation.getCourt().getCourtNumber());
        assertThat(response.surfaceTypeId()).isEqualTo(reservation.getCourt().getSurfaceType().getId());
        assertThat(response.surfaceTypeName()).isEqualTo(reservation.getCourt().getSurfaceType().getName());
        assertThat(response.customerPhoneNumber()).isEqualTo(reservation.getCustomer().getPhoneNumber());
        assertThat(response.customerName()).isEqualTo(reservation.getCustomer().getName());
        assertThat(response.startTime()).isEqualTo(reservation.getStartTime());
        assertThat(response.endTime()).isEqualTo(reservation.getEndTime());
        assertThat(response.gameType()).isEqualTo(reservation.getGameType());
        assertThat(response.price()).isEqualByComparingTo(reservation.getPrice());
        assertThat(response.createdAt()).isEqualTo(reservation.getCreatedAt());
    }

    private Reservation reservation(
            Long id,
            Court court,
            Customer customer,
            LocalDateTime startTime,
            LocalDateTime endTime,
            GameType gameType,
            String price
    ) {
        Reservation reservation = new Reservation();
        setId(reservation, id);
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setGameType(gameType);
        reservation.setPrice(new BigDecimal(price));
        reservation.setCreatedAt(LocalDateTime.of(2026, 6, 1, 9, 0));
        return reservation;
    }

    private Court court(Long id, String courtNumber, SurfaceType surfaceType) {
        Court court = new Court();
        setId(court, id);
        court.setCourtNumber(courtNumber);
        court.setSurfaceType(surfaceType);
        return court;
    }

    private SurfaceType surfaceType(Long id, String name, String pricePerMinute) {
        SurfaceType surfaceType = new SurfaceType();
        setId(surfaceType, id);
        surfaceType.setName(name);
        surfaceType.setPricePerMinute(new BigDecimal(pricePerMinute));
        return surfaceType;
    }

    private Customer customer(Long id, String phoneNumber, String name) {
        Customer customer = new Customer();
        setId(customer, id);
        customer.setPhoneNumber(phoneNumber);
        customer.setName(name);
        return customer;
    }

    private void setId(BaseEntity entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
