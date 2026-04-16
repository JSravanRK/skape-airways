package com.airline.controller;

import com.airline.dto.FlightDto;
import com.airline.enums.FlightStatus;
import com.airline.service.FlightService;
import com.airline.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;
    private final SeatService   seatService;

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String from,
                                    @RequestParam String to,
                                    @RequestParam String date) {
        try {
            return ResponseEntity.ok(flightService.searchFlights(from, to, date));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(flightService.getFlightById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/seats")
    public ResponseEntity<?> getSeats(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = auth != null ? (Long) auth.getDetails() : null;
            if (userId != null) {
                return ResponseEntity.ok(seatService.getSeatMapForUser(id, userId));
            }
            return ResponseEntity.ok(seatService.getSeatMap(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{flightId}/seats/{seatId}/lock")
    public ResponseEntity<?> lockSeat(@PathVariable Long flightId,
                                      @PathVariable Long seatId,
                                      Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            return ResponseEntity.ok(seatService.lockSeat(seatId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{flightId}/seats/{seatId}/lock")
    public ResponseEntity<?> releaseLock(@PathVariable Long flightId,
                                         @PathVariable Long seatId,
                                         Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            seatService.releaseLock(seatId, userId);
            return ResponseEntity.ok(Map.of("message", "Lock released."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Admin only ───────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(flightService.getAllFlights());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody FlightDto.FlightRequest req) {
        try {
            return ResponseEntity.ok(flightService.createFlight(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody FlightDto.FlightRequest req) {
        try {
            return ResponseEntity.ok(flightService.updateFlight(id, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam FlightStatus status) {
        try {
            return ResponseEntity.ok(flightService.updateFlightStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            flightService.deleteFlight(id);
            return ResponseEntity.ok(Map.of("message", "Flight deleted."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
