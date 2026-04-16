package com.airline.controller;

import com.airline.model.Airport;
import com.airline.repository.AirportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportRepository airportRepository;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(airportRepository.findAll());
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String q) {
        return ResponseEntity.ok(
            airportRepository.findByCityContainingIgnoreCaseOrNameContainingIgnoreCase(q, q)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Airport airport) {
        try {
            return ResponseEntity.ok(airportRepository.save(airport));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        airportRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Airport deleted."));
    }
}
