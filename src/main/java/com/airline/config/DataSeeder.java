package com.airline.config;

import com.airline.enums.Role;
import com.airline.model.Airport;
import com.airline.model.User;
import com.airline.repository.AirportRepository;
import com.airline.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository    userRepository;
    private final AirportRepository airportRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JdbcTemplate      jdbcTemplate;

    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            fixSchema();   // MUST run first — before any inserts
            seedAdmin();
            seedAirports();
        };
    }

    /**
     * Fixes leftover/mismatched columns from older schema versions.
     * Every statement is idempotent — safe to run on every startup.
     */
    private void fixSchema() {
        // Fix old 'sky_miles' column (no DEFAULT) that blocks new user inserts
        runSafely(
            "ALTER TABLE users MODIFY COLUMN sky_miles INT NOT NULL DEFAULT 0",
            "sky_miles patched with DEFAULT 0"
        );
        // Ensure miles_balance has DEFAULT 0
        runSafely(
            "ALTER TABLE users MODIFY COLUMN miles_balance INT NOT NULL DEFAULT 0",
            "miles_balance ensured DEFAULT 0"
        );
        // Ensure miles_awarded has DEFAULT 0 on bookings
        runSafely(
            "ALTER TABLE bookings MODIFY COLUMN miles_awarded TINYINT(1) NOT NULL DEFAULT 0",
            "miles_awarded ensured DEFAULT 0"
        );
    }

    private void runSafely(String sql, String successMsg) {
    try {
        jdbcTemplate.execute(sql);
    } catch (Exception ignored) {
        // Column may not exist yet or already correct — silently skip
    }
}

    private void seedAdmin() {
        if (!userRepository.existsByEmail("skape@airways.com")) {
            User admin = User.builder()
                    .name("Sravan Admin")
                    .email("skape@airways.com")
                    .password(passwordEncoder.encode("ska6air"))
                    .role(Role.ADMIN)
                    .phone("9999999999")
                    .build();
            userRepository.save(admin);
            
        }
    }

    private void seedAirports() {
        if (airportRepository.count() > 0) return;
        List<Airport> airports = List.of(
            Airport.builder().code("DEL").name("Indira Gandhi International Airport").city("New Delhi").country("India").build(),
            Airport.builder().code("BOM").name("Chhatrapati Shivaji Maharaj International Airport").city("Mumbai").country("India").build(),
            Airport.builder().code("MAA").name("Chennai International Airport").city("Chennai").country("India").build(),
            Airport.builder().code("BLR").name("Kempegowda International Airport").city("Bangalore").country("India").build(),
            Airport.builder().code("HYD").name("Rajiv Gandhi International Airport").city("Hyderabad").country("India").build(),
            Airport.builder().code("CCU").name("Netaji Subhas Chandra Bose International Airport").city("Kolkata").country("India").build(),
            Airport.builder().code("COK").name("Cochin International Airport").city("Kochi").country("India").build(),
            Airport.builder().code("AMD").name("Sardar Vallabhbhai Patel International Airport").city("Ahmedabad").country("India").build(),
            Airport.builder().code("GOI").name("Goa International Airport").city("Goa").country("India").build(),
            Airport.builder().code("PNQ").name("Pune Airport").city("Pune").country("India").build(),
            Airport.builder().code("JAI").name("Jaipur International Airport").city("Jaipur").country("India").build(),
            Airport.builder().code("LKO").name("Chaudhary Charan Singh International Airport").city("Lucknow").country("India").build()
        );
        airportRepository.saveAll(airports);
        
    }
}
