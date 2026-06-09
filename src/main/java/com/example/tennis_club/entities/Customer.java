package com.example.tennis_club.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_phone_number", columnNames = "phone_number"))
public class Customer extends BaseEntity {
    private static final int MAX_PHONE_NUMBER_LENGTH = 16;

    @Column(name = "phone_number", nullable = false, length = MAX_PHONE_NUMBER_LENGTH)
    private String phoneNumber;

    @Column(nullable = false, length = 30)
    private String name;
}
