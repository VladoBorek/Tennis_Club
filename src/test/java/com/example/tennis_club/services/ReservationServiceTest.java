package com.example.tennis_club.services;

import com.example.tennis_club.daos.ReservationDao;
import com.example.tennis_club.entities.*;
import com.example.tennis_club.exceptions.BadRequestException;
import com.example.tennis_club.exceptions.NotFoundException;
import com.example.tennis_club.models.GameType;
import com.example.tennis_club.models.ReservationCommand;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationDao reservationDao;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void getActiveReservationByIdShouldReturnReservationWhenFound() {
        Reservation reservation = reservation(1L, court(10L, "COURT-1", "0.30"), customer(20L));

        when(reservationDao.findActiveById(1L)).thenReturn(Optional.of(reservation));

        Reservation result = reservationService.getActiveReservationById(1L);

        assertThat(result).isSameAs(reservation);
    }

    @Test
    void getActiveReservationByIdShouldThrowWhenReservationDoesNotExist() {
        when(reservationDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getActiveReservationById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Reservation not found");
    }

    @Test
    void getReservationsByCourtNumberShouldReturnDaoResult() {
        Reservation first = reservation(1L, court(10L, "COURT-1", "0.30"), customer(20L));
        Reservation second = reservation(2L, court(10L, "COURT-1", "0.30"), customer(21L));

        when(reservationDao.findActiveByCourtNumberOrderByCreatedAt("COURT-1"))
                .thenReturn(List.of(first, second));

        List<Reservation> result = reservationService.getReservationsByCourtNumber("COURT-1");

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void getReservationsByCustomerPhoneNumberShouldReturnAllReservationsWhenFutureOnlyIsFalse() {
        Reservation reservation = reservation(1L, court(10L, "COURT-1", "0.30"), customer(20L));

        when(reservationDao.findActiveByCustomerPhoneNumber("+421900000000"))
                .thenReturn(List.of(reservation));

        List<Reservation> result = reservationService.getReservationsByCustomerPhoneNumber("+421900000000", false);

        assertThat(result).containsExactly(reservation);
        verify(reservationDao).findActiveByCustomerPhoneNumber("+421900000000");
        verify(reservationDao, never()).findFutureActiveByCustomerPhoneNumber(any(), any());
    }

    @Test
    void getReservationsByCustomerPhoneNumberShouldReturnFutureReservationsWhenFutureOnlyIsTrue() {
        Reservation reservation = reservation(1L, court(10L, "COURT-1", "0.30"), customer(20L));

        when(reservationDao.findFutureActiveByCustomerPhoneNumber(eq("+421900000000"), any(LocalDateTime.class)))
                .thenReturn(List.of(reservation));

        List<Reservation> result = reservationService.getReservationsByCustomerPhoneNumber("+421900000000", true);

        assertThat(result).containsExactly(reservation);
        verify(reservationDao).findFutureActiveByCustomerPhoneNumber(eq("+421900000000"), any(LocalDateTime.class));
        verify(reservationDao, never()).findActiveByCustomerPhoneNumber(any());
    }

    @Test
    void createReservationShouldCreateSinglesReservationWithCalculatedPrice() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 11, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.SINGLES);

        when(reservationDao.existsOverlappingActiveReservation(10L, startTime, endTime)).thenReturn(false);
        when(reservationDao.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.createReservation(command);

        assertThat(result.getCourt()).isSameAs(court);
        assertThat(result.getCustomer()).isSameAs(customer);
        assertThat(result.getStartTime()).isEqualTo(startTime);
        assertThat(result.getEndTime()).isEqualTo(endTime);
        assertThat(result.getGameType()).isEqualTo(GameType.SINGLES);
        assertThat(result.getPrice()).isEqualByComparingTo("18.00");
    }

    @Test
    void createReservationShouldCreateDoublesReservationWithCalculatedPriceMultiplier() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 11, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.DOUBLES);

        when(reservationDao.existsOverlappingActiveReservation(10L, startTime, endTime)).thenReturn(false);
        when(reservationDao.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.createReservation(command);

        assertThat(result.getPrice()).isEqualByComparingTo("27.00");
    }

    @Test
    void createReservationShouldAllowReservationStartingWhenAnotherEnds() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 11, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 12, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.SINGLES);

        when(reservationDao.existsOverlappingActiveReservation(10L, startTime, endTime)).thenReturn(false);
        when(reservationDao.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.createReservation(command);

        assertThat(result.getStartTime()).isEqualTo(startTime);
        assertThat(result.getEndTime()).isEqualTo(endTime);
        verify(reservationDao).save(any(Reservation.class));
    }

    @Test
    void createReservationShouldThrowWhenStartTimeEqualsEndTime() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, startTime, GameType.SINGLES);

        assertThatThrownBy(() -> reservationService.createReservation(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reservation start time must be before end time");

        verify(reservationDao, never()).existsOverlappingActiveReservation(anyLong(), any(), any());
        verify(reservationDao, never()).save(any());
    }

    @Test
    void createReservationShouldThrowWhenStartTimeIsAfterEndTime() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 12, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.SINGLES);

        assertThatThrownBy(() -> reservationService.createReservation(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reservation start time must be before end time");

        verify(reservationDao, never()).existsOverlappingActiveReservation(anyLong(), any(), any());
        verify(reservationDao, never()).save(any());
    }

    @Test
    void createReservationShouldThrowWhenReservationOverlaps() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 11, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.SINGLES);

        when(reservationDao.existsOverlappingActiveReservation(10L, startTime, endTime)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reservation overlaps with an existing reservation");

        verify(reservationDao, never()).save(any());
    }

    @Test
    void updateReservationShouldUpdateReservationAndRecalculatePrice() {
        Court oldCourt = court(10L, "COURT-1", "0.30");
        Court newCourt = court(11L, "COURT-2", "0.50");
        Customer oldCustomer = customer(20L);
        Customer newCustomer = customer(21L);
        Reservation existingReservation = reservation(1L, oldCourt, oldCustomer);

        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 14, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 16, 0);
        ReservationCommand command = new ReservationCommand(newCourt, newCustomer, startTime, endTime, GameType.DOUBLES);

        when(reservationDao.findActiveById(1L)).thenReturn(Optional.of(existingReservation));
        when(reservationDao.existsOverlappingActiveReservationExcludingId(1L, 11L, startTime, endTime)).thenReturn(false);
        when(reservationDao.save(existingReservation)).thenReturn(existingReservation);

        Reservation result = reservationService.updateReservation(1L, command);

        assertThat(result).isSameAs(existingReservation);
        assertThat(result.getCourt()).isSameAs(newCourt);
        assertThat(result.getCustomer()).isSameAs(newCustomer);
        assertThat(result.getStartTime()).isEqualTo(startTime);
        assertThat(result.getEndTime()).isEqualTo(endTime);
        assertThat(result.getGameType()).isEqualTo(GameType.DOUBLES);
        assertThat(result.getPrice()).isEqualByComparingTo("90.00");
    }

    @Test
    void updateReservationShouldThrowWhenReservationDoesNotExist() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 11, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.SINGLES);

        when(reservationDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.updateReservation(1L, command))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Reservation not found");

        verify(reservationDao, never()).existsOverlappingActiveReservationExcludingId(anyLong(), anyLong(), any(), any());
        verify(reservationDao, never()).save(any());
    }

    @Test
    void updateReservationShouldThrowWhenUpdatedIntervalIsInvalid() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        Reservation existingReservation = reservation(1L, court, customer);

        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 12, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.SINGLES);

        when(reservationDao.findActiveById(1L)).thenReturn(Optional.of(existingReservation));

        assertThatThrownBy(() -> reservationService.updateReservation(1L, command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reservation start time must be before end time");

        verify(reservationDao, never()).existsOverlappingActiveReservationExcludingId(anyLong(), anyLong(), any(), any());
        verify(reservationDao, never()).save(any());
    }

    @Test
    void updateReservationShouldThrowWhenUpdatedReservationOverlaps() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        Reservation existingReservation = reservation(1L, court, customer);

        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 11, 0);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.SINGLES);

        when(reservationDao.findActiveById(1L)).thenReturn(Optional.of(existingReservation));
        when(reservationDao.existsOverlappingActiveReservationExcludingId(1L, 10L, startTime, endTime)).thenReturn(true);

        assertThatThrownBy(() -> reservationService.updateReservation(1L, command))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Reservation overlaps with an existing reservation");

        verify(reservationDao, never()).save(any());
    }

    @Test
    void updateReservationShouldCheckOverlapAgainstNewCourt() {
        Court oldCourt = court(10L, "COURT-1", "0.30");
        Court newCourt = court(11L, "COURT-2", "0.30");
        Customer customer = customer(20L);
        Reservation existingReservation = reservation(1L, oldCourt, customer);

        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 11, 0);
        ReservationCommand command = new ReservationCommand(newCourt, customer, startTime, endTime, GameType.SINGLES);

        when(reservationDao.findActiveById(1L)).thenReturn(Optional.of(existingReservation));
        when(reservationDao.existsOverlappingActiveReservationExcludingId(1L, 11L, startTime, endTime)).thenReturn(false);
        when(reservationDao.save(existingReservation)).thenReturn(existingReservation);

        reservationService.updateReservation(1L, command);

        verify(reservationDao).existsOverlappingActiveReservationExcludingId(1L, 11L, startTime, endTime);
    }

    @Test
    void deleteReservationShouldSoftDeleteReservationWhenFound() {
        Reservation reservation = reservation(1L, court(10L, "COURT-1", "0.30"), customer(20L));

        when(reservationDao.findActiveById(1L)).thenReturn(Optional.of(reservation));

        reservationService.deleteReservation(1L);

        verify(reservationDao).softDelete(reservation);
    }

    @Test
    void deleteReservationShouldThrowWhenReservationDoesNotExist() {
        when(reservationDao.findActiveById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.deleteReservation(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Reservation not found");

        verify(reservationDao, never()).softDelete(any());
    }

    @Test
    void createReservationShouldPassCreatedReservationToDao() {
        Court court = court(10L, "COURT-1", "0.30");
        Customer customer = customer(20L);
        LocalDateTime startTime = LocalDateTime.of(2026, 6, 10, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 6, 10, 10, 30);
        ReservationCommand command = new ReservationCommand(court, customer, startTime, endTime, GameType.SINGLES);

        when(reservationDao.existsOverlappingActiveReservation(10L, startTime, endTime)).thenReturn(false);
        when(reservationDao.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        reservationService.createReservation(command);

        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationDao).save(captor.capture());

        Reservation savedReservation = captor.getValue();
        assertThat(savedReservation.getCourt()).isSameAs(court);
        assertThat(savedReservation.getCustomer()).isSameAs(customer);
        assertThat(savedReservation.getStartTime()).isEqualTo(startTime);
        assertThat(savedReservation.getEndTime()).isEqualTo(endTime);
        assertThat(savedReservation.getGameType()).isEqualTo(GameType.SINGLES);
        assertThat(savedReservation.getPrice()).isEqualByComparingTo("9.00");
    }

    private Reservation reservation(Long id, Court court, Customer customer) {
        Reservation reservation = new Reservation();
        setId(reservation, id);
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setStartTime(LocalDateTime.of(2026, 6, 10, 9, 0));
        reservation.setEndTime(LocalDateTime.of(2026, 6, 10, 10, 0));
        reservation.setGameType(GameType.SINGLES);
        reservation.setPrice(new BigDecimal("18.00"));
        return reservation;
    }

    private Court court(Long id, String courtNumber, String pricePerMinute) {
        SurfaceType surfaceType = surfaceType(100L + id, pricePerMinute);

        Court court = new Court();
        setId(court, id);
        court.setCourtNumber(courtNumber);
        court.setSurfaceType(surfaceType);
        return court;
    }

    private SurfaceType surfaceType(Long id, String pricePerMinute) {
        SurfaceType surfaceType = new SurfaceType();
        setId(surfaceType, id);
        surfaceType.setName("Surface " + id);
        surfaceType.setPricePerMinute(new BigDecimal(pricePerMinute));
        return surfaceType;
    }

    private Customer customer(Long id) {
        Customer customer = new Customer();
        setId(customer, id);
        customer.setPhoneNumber("+421900000" + id);
        customer.setName("Customer " + id);
        return customer;
    }

    private void setId(BaseEntity entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}