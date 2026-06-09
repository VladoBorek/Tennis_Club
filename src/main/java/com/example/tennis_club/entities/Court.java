package com.example.tennis_club.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "court",
        uniqueConstraints = @UniqueConstraint(name = "uk_court_number", columnNames = "court_number")
)
public class Court {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "court_number", nullable = false, length = 50)
    private String courtNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "surface_id", nullable = false)
    private Surface surface;

    @Column(nullable = false)
    private boolean deleted = false;
}
