package com.airline.dto;

import com.airline.enums.BookingStatus;
import lombok.Data;
import java.time.LocalDateTime;

public class BookingDto {

    @Data
    public static class BookingRequest {
        private Long flightId;
        private Long seatId;
        private String passengerName;
        private Integer passengerAge;
        private String passengerGender;
        private String passengerPassport;
        /** When true, charge economy price for a Business seat (upgrade voucher) */
        private boolean upgradeVoucherActive;
    }

    @Data
    public static class BookingResponse {
        private Long id;
        private String bookingReference;
        private BookingStatus status;
        private double totalAmount;
        private String flightNumber;
        private String fromCode;   // FIX: airport code (e.g. DEL) — used on confirmation page
        private String fromCity;
        private String toCode;     // FIX: airport code (e.g. BOM)
        private String toCity;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private String seatNumber;
        private String seatClass;
        private String passengerName;
        private String passengerAge;
        private String passengerGender;
        private LocalDateTime createdAt;
        private String airlineName;
    }
}
