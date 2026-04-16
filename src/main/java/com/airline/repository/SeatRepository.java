package com.airline.repository;

import com.airline.enums.SeatClass;
import com.airline.model.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByFlightIdOrderBySeatNumber(Long flightId);

    List<Seat> findByFlightIdAndSeatClass(Long flightId, SeatClass seatClass);

    // Pessimistic write lock to prevent concurrent seat selection
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithLock(@Param("id") Long id);

    // Release all expired locks (scheduled job)
    @Modifying
    @Query("UPDATE Seat s SET s.locked = false, s.lockedByUserId = null, s.lockExpiresAt = null " +
           "WHERE s.locked = true AND s.lockExpiresAt < :now AND s.booked = false")
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    // Release all locks held by a specific user
    @Modifying
    @Query("UPDATE Seat s SET s.locked = false, s.lockedByUserId = null, s.lockExpiresAt = null " +
           "WHERE s.lockedByUserId = :userId AND s.booked = false")
    int releaseUserLocks(@Param("userId") Long userId);

    long countByFlightIdAndBooked(Long flightId, boolean booked);

    @Query("SELECT s FROM Seat s WHERE s.flight.id = :flightId AND s.booked = false AND " +
           "(s.locked = false OR s.lockExpiresAt < :now)")
    List<Seat> findAvailableSeats(@Param("flightId") Long flightId, @Param("now") LocalDateTime now);
}
