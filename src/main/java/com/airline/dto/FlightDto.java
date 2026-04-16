package com.airline.dto;

import com.airline.enums.FlightStatus;
import lombok.Data;
import java.time.LocalDateTime;

public class FlightDto {

    @Data
    public static class FlightRequest {
        private String flightNumber;
        private Long fromAirportId;
        private Long toAirportId;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private int totalSeats;
        private double economyPrice;
        private double businessPrice;
        private double firstClassPrice;
        private String airlineName;
        private String aircraftType;
    }

    @Data
    public static class FlightResponse {
        private Long id;
        private String flightNumber;
        private String fromCode;
        private String fromCity;
        private String toCode;
        private String toCity;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private int availableSeats;
        private int totalSeats;

        // Base prices (set by admin)
        private double economyPrice;
        private double businessPrice;
        private double firstClassPrice;

        // Dynamic prices (calculated in real-time based on demand + time)
        private double dynamicEconomyPrice;
        private double dynamicBusinessPrice;
        private double dynamicFirstClassPrice;

        // Demand metadata for UI display
        private String demandLabel;       // LOW_DEMAND / AVAILABLE / FILLING_UP / HIGH_DEMAND / SELLING_FAST
        private int priceChangePercent;   // e.g. 25 means "+25% vs base"

        private String airlineName;
        private String aircraftType;
        private FlightStatus status;
        private long durationMinutes;
    }

    @Data
    public static class SearchRequest {
        private String fromCode;
        private String toCode;
        private String date;
    }
}
