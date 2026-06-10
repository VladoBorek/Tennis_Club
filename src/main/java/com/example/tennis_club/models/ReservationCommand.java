package com.example.tennis_club.models;

import com.example.tennis_club.entities.Court;
import com.example.tennis_club.entities.Customer;

import java.time.LocalDateTime;

public record ReservationCommand(
        Court court,
        Customer customer,
        LocalDateTime startTime,
        LocalDateTime endTime,
        GameType gameType
) {
}
