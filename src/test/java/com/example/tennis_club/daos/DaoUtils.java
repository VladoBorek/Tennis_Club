package com.example.tennis_club.daos;


import com.example.tennis_club.entities.Court;
import com.example.tennis_club.entities.Customer;
import com.example.tennis_club.entities.Reservation;
import com.example.tennis_club.entities.SurfaceType;
import com.example.tennis_club.models.GameType;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootTest
@Transactional
public class DaoUtils {

    @Autowired
    protected EntityManager entityManager;

    protected SurfaceType createSurfaceType(String name, BigDecimal pricePerMinute) {
        SurfaceType surfaceType = new SurfaceType();
        surfaceType.setName(name);
        surfaceType.setPricePerMinute(pricePerMinute);
        entityManager.persist(surfaceType);
        return surfaceType;
    }

    protected Customer createCustomer(String phoneNumber, String name) {
        Customer customer = new Customer();
        customer.setPhoneNumber(phoneNumber);
        customer.setName(name);
        entityManager.persist(customer);
        return customer;
    }

    protected Court createCourt(String courtNumber, SurfaceType surfaceType) {
        Court court = new Court();
        court.setCourtNumber(courtNumber);
        court.setSurfaceType(surfaceType);
        entityManager.persist(court);
        return court;
    }

    protected Reservation createReservation(
            Court court,
            Customer customer,
            LocalDateTime startTime,
            LocalDateTime endTime,
            GameType gameType,
            BigDecimal price,
            LocalDateTime createdAt
    ) {
        Reservation reservation = new Reservation();
        reservation.setCourt(court);
        reservation.setCustomer(customer);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setGameType(gameType);
        reservation.setPrice(price);
        reservation.setCreatedAt(createdAt);
        entityManager.persist(reservation);
        return reservation;
    }

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

}
