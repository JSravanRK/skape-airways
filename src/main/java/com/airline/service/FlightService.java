package com.airline.service;

import com.airline.dto.FlightDto;
import com.airline.enums.FlightStatus;
import com.airline.enums.SeatClass;
import com.airline.model.Airport;
import com.airline.model.Flight;
import com.airline.model.Booking;
import com.airline.model.Seat;
import com.airline.repository.AirportRepository;
import com.airline.repository.FlightRepository;
import com.airline.repository.BookingRepository;
import com.airline.repository.PaymentRepository;
import com.airline.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository      flightRepository;
    private final AirportRepository     airportRepository;
    private final SeatRepository        seatRepository;
    private final DynamicPricingService  dynamicPricingService;
    private final BookingRepository      bookingRepository;
    private final PaymentRepository      paymentRepository;

    @Transactional
    public FlightDto.FlightResponse createFlight(FlightDto.FlightRequest req) {
        Airport from = airportRepository.findById(req.getFromAirportId())
                .orElseThrow(() -> new RuntimeException("From airport not found"));
        Airport to = airportRepository.findById(req.getToAirportId())
                .orElseThrow(() -> new RuntimeException("To airport not found"));

        if (flightRepository.findByFlightNumber(req.getFlightNumber()).isPresent()) {
            throw new RuntimeException("Flight number already exists");
        }

        Flight flight = Flight.builder()
                .flightNumber(req.getFlightNumber())
                .fromAirport(from).toAirport(to)
                .departureTime(req.getDepartureTime())
                .arrivalTime(req.getArrivalTime())
                .totalSeats(req.getTotalSeats())
                .availableSeats(req.getTotalSeats())
                .economyPrice(req.getEconomyPrice())
                .businessPrice(req.getBusinessPrice())
                .firstClassPrice(req.getFirstClassPrice())
                .airlineName(req.getAirlineName())
                .aircraftType(req.getAircraftType())
                .status(FlightStatus.SCHEDULED)
                .build();
        flight = flightRepository.save(flight);
        generateSeats(flight, req.getTotalSeats());
        return toResponse(flight);
    }

    private void generateSeats(Flight flight, int total) {
        List<Seat> seats = new ArrayList<>();
        int firstCount    = Math.max(1, (int)(total * 0.10));
        int businessCount = Math.max(1, (int)(total * 0.20));
        int economyCount  = total - firstCount - businessCount;
        String[] cols = {"A","B","C","D","E","F"};
        int row = 1, count = 0;

        for (int i = 0; i < firstCount; i++) {
            seats.add(Seat.builder().flight(flight).seatNumber(row + cols[count % 6])
                    .seatClass(SeatClass.FIRST_CLASS).build());
            count++; if (count % 6 == 0) row++;
        }
        for (int i = 0; i < businessCount; i++) {
            seats.add(Seat.builder().flight(flight).seatNumber(row + cols[count % 6])
                    .seatClass(SeatClass.BUSINESS).build());
            count++; if (count % 6 == 0) row++;
        }
        for (int i = 0; i < economyCount; i++) {
            seats.add(Seat.builder().flight(flight).seatNumber(row + cols[count % 6])
                    .seatClass(SeatClass.ECONOMY).build());
            count++; if (count % 6 == 0) row++;
        }
        seatRepository.saveAll(seats);
    }

    public List<FlightDto.FlightResponse> searchFlights(String fromCode, String toCode, String date) {
        Airport from = airportRepository.findByCode(fromCode)
                .orElseThrow(() -> new RuntimeException("Origin airport not found: " + fromCode));
        Airport to = airportRepository.findByCode(toCode)
                .orElseThrow(() -> new RuntimeException("Destination airport not found: " + toCode));
        LocalDate travelDate = LocalDate.parse(date);
        LocalDateTime startOfDay = travelDate.atStartOfDay();
        LocalDateTime endOfDay   = travelDate.atTime(LocalTime.MAX);
        return flightRepository.searchFlights(from.getId(), to.getId(), startOfDay, endOfDay)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public FlightDto.FlightResponse getFlightById(Long id) {
        return toResponse(flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found")));
    }

    public List<FlightDto.FlightResponse> getAllFlights() {
        return flightRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public FlightDto.FlightResponse updateFlightStatus(Long id, FlightStatus status) {
        Flight f = flightRepository.findById(id).orElseThrow(() -> new RuntimeException("Flight not found"));
        f.setStatus(status);
        return toResponse(flightRepository.save(f));
    }

    @Transactional
    public FlightDto.FlightResponse updateFlight(Long id, FlightDto.FlightRequest req) {
        Flight f = flightRepository.findById(id).orElseThrow(() -> new RuntimeException("Flight not found"));
        f.setDepartureTime(req.getDepartureTime());
        f.setArrivalTime(req.getArrivalTime());
        f.setEconomyPrice(req.getEconomyPrice());
        f.setBusinessPrice(req.getBusinessPrice());
        f.setFirstClassPrice(req.getFirstClassPrice());
        if (req.getAirlineName() != null) f.setAirlineName(req.getAirlineName());
        return toResponse(flightRepository.save(f));
    }

    @Transactional
    public void deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        // FIX: Full cascade delete in the correct FK dependency order:
        //   payments → bookings → seats → flight
        // MySQL does not cascade automatically because the FKs have no ON DELETE CASCADE.

        // 1. Find all bookings for this flight
        List<Booking> bookings = bookingRepository.findByFlightId(id);

        // 2. Delete all payments linked to those bookings
        for (Booking booking : bookings) {
            paymentRepository.findByBookingId(booking.getId())
                    .ifPresent(paymentRepository::delete);
        }

        // 3. Delete all bookings for this flight
        bookingRepository.deleteAll(bookings);

        // 4. Delete all seats for this flight
        List<Seat> seats = seatRepository.findByFlightIdOrderBySeatNumber(id);
        seatRepository.deleteAll(seats);

        // 5. Now safe to delete the flight
        flightRepository.delete(flight);
    }

    // ── toResponse with dynamic pricing ─────────────────────────────────────
    private FlightDto.FlightResponse toResponse(Flight f) {
        FlightDto.FlightResponse r = new FlightDto.FlightResponse();
        r.setId(f.getId());
        r.setFlightNumber(f.getFlightNumber());
        r.setFromCode(f.getFromAirport().getCode());
        r.setFromCity(f.getFromAirport().getCity());
        r.setToCode(f.getToAirport().getCode());
        r.setToCity(f.getToAirport().getCity());
        r.setDepartureTime(f.getDepartureTime());
        r.setArrivalTime(f.getArrivalTime());
        r.setAvailableSeats(f.getAvailableSeats());
        r.setTotalSeats(f.getTotalSeats());
        r.setEconomyPrice(f.getEconomyPrice());
        r.setBusinessPrice(f.getBusinessPrice());
        r.setFirstClassPrice(f.getFirstClassPrice());
        r.setAirlineName(f.getAirlineName());
        r.setAircraftType(f.getAircraftType());
        r.setStatus(f.getStatus());
        r.setDurationMinutes(ChronoUnit.MINUTES.between(f.getDepartureTime(), f.getArrivalTime()));

        // ── Dynamic pricing ──────────────────────────────────────────────────
        double dynEco = dynamicPricingService.calculateDynamicPrice(
                f.getEconomyPrice(), f.getTotalSeats(), f.getAvailableSeats(), f.getDepartureTime());
        double dynBiz = dynamicPricingService.calculateDynamicPrice(
                f.getBusinessPrice(), f.getTotalSeats(), f.getAvailableSeats(), f.getDepartureTime());
        double dynFirst = dynamicPricingService.calculateDynamicPrice(
                f.getFirstClassPrice(), f.getTotalSeats(), f.getAvailableSeats(), f.getDepartureTime());

        r.setDynamicEconomyPrice(dynEco);
        r.setDynamicBusinessPrice(dynBiz);
        r.setDynamicFirstClassPrice(dynFirst);
        r.setDemandLabel(dynamicPricingService.getDemandLabel(f.getTotalSeats(), f.getAvailableSeats()));
        r.setPriceChangePercent(dynamicPricingService.getPriceChangePercent(f.getEconomyPrice(), dynEco));

        return r;
    }
}
