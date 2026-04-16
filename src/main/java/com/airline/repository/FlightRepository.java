package com.airline.repository;

import com.airline.enums.FlightStatus;
import com.airline.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    @Query("SELECT f FROM Flight f WHERE " +
           "f.fromAirport.id = :fromId AND f.toAirport.id = :toId AND " +
           "f.departureTime >= :startOfDay AND f.departureTime < :endOfDay AND " +
           "f.status != 'CANCELLED' AND f.availableSeats > 0 " +
           "ORDER BY f.departureTime ASC")
    List<Flight> searchFlights(
        @Param("fromId") Long fromId,
        @Param("toId") Long toId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    List<Flight> findByStatus(FlightStatus status);

    @Query("SELECT f FROM Flight f WHERE f.departureTime BETWEEN :start AND :end ORDER BY f.departureTime")
    List<Flight> findFlightsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(f) FROM Flight f WHERE f.status = 'CANCELLED'")
    long countCancelled();

    @Query("SELECT f.fromAirport.city, f.toAirport.city, COUNT(b) as cnt " +
           "FROM Flight f JOIN Booking b ON b.flight.id = f.id " +
           "WHERE b.status = 'CONFIRMED' " +
           "GROUP BY f.fromAirport.city, f.toAirport.city " +
           "ORDER BY cnt DESC")
    List<Object[]> findPopularRoutes();
}
