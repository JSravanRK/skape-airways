package com.airline.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "airports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Airport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 3)
    private String code;          // IATA code e.g. DEL, BOM, MAA

    @Column(nullable = false)
    private String name;          // Indira Gandhi International Airport

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;
}
