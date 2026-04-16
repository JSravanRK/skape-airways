package com.airline.model;

import com.airline.enums.FlightStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "flights")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_number", nullable = false, unique = true)
    private String flightNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "from_airport_id", nullable = false)
    private Airport fromAirport;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "to_airport_id", nullable = false)
    private Airport toAirport;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(name = "economy_price", nullable = false)
    private double economyPrice;

    @Column(name = "business_price", nullable = false)
    private double businessPrice;

    @Column(name = "first_class_price", nullable = false)
    private double firstClassPrice;

    @Column(name = "airline_name")
    private String airlineName;

    @Column(name = "aircraft_type")
    private String aircraftType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightStatus status;

    @Version
    private Long version;   // Optimistic locking

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = FlightStatus.SCHEDULED;
    }
}
