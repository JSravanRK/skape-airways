package com.airline.model;

import com.airline.enums.SeatClass;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_seat_flight", columnList = "flight_id"),
    @Index(name = "idx_seat_number", columnList = "flight_id, seat_number")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "seat_number", nullable = false, length = 5)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_class", nullable = false)
    private SeatClass seatClass;

    // @Builder.Default ensures Lombok's @Builder respects these initial values
    @Builder.Default
    @Column(name = "is_booked", nullable = false)
    private boolean booked = false;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Column(name = "locked_by_user_id")
    private Long lockedByUserId;

    @Column(name = "lock_expires_at")
    private LocalDateTime lockExpiresAt;

    @Version
    private Long version;  // Optimistic locking
}
