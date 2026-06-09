package com.example.tennis_club.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_phone_number", columnNames = "phone_number"))
public class Customer {
    private final int max_phone_number_standard_length = 16;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = max_phone_number_standard_length)
    private String phoneNumber;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false)
    private boolean deleted;
}
