package com.example.tennis_club.dao;

import com.example.tennis_club.entities.Reservation;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ReservationDao extends BaseDao<Reservation> {

    public ReservationDao() {
        super(Reservation.class);
    }

    public List<Reservation> findActiveByCourtNumberOrderByCreatedAt(String courtNumber) {
        return entityManager.createQuery("""
                        select reservation
                        from Reservation reservation
                        join fetch reservation.court court
                        join fetch reservation.customer customer
                        where court.courtNumber = :courtNumber
                          and reservation.deleted = false
                        order by reservation.createdAt asc
                        """, Reservation.class)
                .setParameter("courtNumber", courtNumber)
                .getResultList();
    }

    public List<Reservation> findActiveByCustomerPhoneNumber(String phoneNumber) {
        return entityManager.createQuery("""
                        select reservation
                        from Reservation reservation
                        join fetch reservation.court court
                        join fetch court.surfaceType
                        join fetch reservation.customer customer
                        where customer.phoneNumber = :phoneNumber
                          and reservation.deleted = false
                        order by reservation.startTime asc
                        """, Reservation.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList();
    }

    public List<Reservation> findFutureActiveByCustomerPhoneNumber(String phoneNumber, LocalDateTime now) {
        return entityManager.createQuery("""
                        select reservation
                        from Reservation reservation
                        join fetch reservation.court court
                        join fetch court.surfaceType
                        join fetch reservation.customer customer
                        where customer.phoneNumber = :phoneNumber
                          and reservation.startTime > :now
                          and reservation.deleted = false
                        order by reservation.startTime asc
                        """, Reservation.class)
                .setParameter("phoneNumber", phoneNumber)
                .setParameter("now", now)
                .getResultList();
    }

    public boolean existsOverlappingActiveReservation(Long courtId, LocalDateTime startTime, LocalDateTime endTime) {
        Long count = entityManager.createQuery("""
                        select count(reservation)
                        from Reservation reservation
                        where reservation.court.id = :courtId
                          and reservation.deleted = false
                          and reservation.startTime < :endTime
                          and reservation.endTime > :startTime
                        """, Long.class)
                .setParameter("courtId", courtId)
                .setParameter("startTime", startTime)
                .setParameter("endTime", endTime)
                .getSingleResult();

        return count > 0;
    }
}