package com.example.tennis_club.entities;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "surface",
        uniqueConstraints = @UniqueConstraint(name = "uk_surface_name", columnNames = "name")
)
public class Surface extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "price_per_minute", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerMinute;
}

