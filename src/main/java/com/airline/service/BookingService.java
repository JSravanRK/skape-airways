package com.airline.service;

import com.airline.dto.BookingDto;
import com.airline.enums.BookingStatus;
import com.airline.model.*;
import com.airline.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository      bookingRepository;
    private final FlightRepository       flightRepository;
    private final SeatRepository         seatRepository;
    private final UserRepository         userRepository;
    private final DynamicPricingService  dynamicPricingService;
    private final EmailService           emailService;
    private final MilesService           milesService;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingDto.BookingResponse createBooking(Long userId, BookingDto.BookingRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Flight flight = flightRepository.findById(req.getFlightId())
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        if (flight.getAvailableSeats() <= 0)
            throw new RuntimeException("No seats available on this flight.");

        Seat seat = seatRepository.findByIdWithLock(req.getSeatId())
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (seat.isBooked())
            throw new RuntimeException("Seat is already booked. Please choose another.");
        if (!flight.getId().equals(seat.getFlight().getId()))
            throw new RuntimeException("Seat does not belong to this flight.");
        if (seat.isLocked() && seat.getLockExpiresAt() != null
                && seat.getLockExpiresAt().isAfter(LocalDateTime.now())
                && !userId.equals(seat.getLockedByUserId()))
            throw new RuntimeException("Seat is held by another user.");

        // ── Dynamic price at booking time ────────────────────────────────────
        double basePrice = switch (seat.getSeatClass()) {
            case FIRST_CLASS -> flight.getFirstClassPrice();
            case BUSINESS    -> flight.getBusinessPrice();
            default          -> flight.getEconomyPrice();
        };
        double dynamicPrice = dynamicPricingService.calculateDynamicPrice(
                basePrice, flight.getTotalSeats(), flight.getAvailableSeats(), flight.getDepartureTime());

        // Upgrade voucher: charge economy price for Business seat
        if (req.isUpgradeVoucherActive() && seat.getSeatClass().name().equals("BUSINESS")) {
            double economyBase    = flight.getEconomyPrice();
            double economyDynamic = dynamicPricingService.calculateDynamicPrice(
                    economyBase, flight.getTotalSeats(), flight.getAvailableSeats(), flight.getDepartureTime());
            dynamicPrice = economyDynamic;
        }

        // Mark seat booked, clear lock
        seat.setBooked(true);
        seat.setLocked(false);
        seat.setLockedByUserId(null);
        seat.setLockExpiresAt(null);
        seatRepository.save(seat);

        flight.setAvailableSeats(flight.getAvailableSeats() - 1);
        flightRepository.save(flight);

        Booking booking = Booking.builder()
                .user(user).flight(flight).seat(seat)
                .bookingReference(generateRef())
                .status(BookingStatus.PENDING)
                .totalAmount(dynamicPrice)
                .passengerName(req.getPassengerName())
                .passengerAge(req.getPassengerAge())
                .passengerGender(req.getPassengerGender())
                .passengerPassport(req.getPassengerPassport())
                .build();
        booking = bookingRepository.save(booking);

        emailService.sendBookingConfirmation(booking);

        return toResponse(booking);
    }

    public List<BookingDto.BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public BookingDto.BookingResponse getBookingByRef(String ref) {
        return toResponse(bookingRepository.findByBookingReference(ref)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + ref)));
    }

    public BookingDto.BookingResponse getBookingById(Long id) {
        return toResponse(bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found")));
    }

    @Transactional
    public BookingDto.BookingResponse cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!booking.getUser().getId().equals(userId))
            throw new RuntimeException("Unauthorized to cancel this booking.");
        if (booking.getStatus() == BookingStatus.CANCELLED)
            throw new RuntimeException("Booking is already cancelled.");

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Seat seat = booking.getSeat();
        seat.setBooked(false);
        seatRepository.save(seat);

        Flight flight = booking.getFlight();
        flight.setAvailableSeats(flight.getAvailableSeats() + 1);
        flightRepository.save(flight);

        // Reverse miles if already awarded
        if (booking.isMilesAwarded()) {
            int milesEarned = (int) Math.round(booking.getTotalAmount() / 10.0);
            milesService.addMiles(userId, -milesEarned,
                    "Cancelled: Flight " + booking.getFlight().getFlightNumber()
                    + " — Seat " + booking.getSeat().getSeatNumber());
        }

        emailService.sendCancellationEmail(booking);

        return toResponse(booking);
    }

    @Transactional
    public void confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    public List<BookingDto.BookingResponse> getAllBookings() {
        return bookingRepository.findAllOrderByCreatedAtDesc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private String generateRef() {
        return "SK" + UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase();
    }

    // FIX: populate fromCode and toCode so confirmation page can show real IATA codes
    private BookingDto.BookingResponse toResponse(Booking b) {
        BookingDto.BookingResponse r = new BookingDto.BookingResponse();
        r.setId(b.getId());
        r.setBookingReference(b.getBookingReference());
        r.setStatus(b.getStatus());
        r.setTotalAmount(b.getTotalAmount());
        r.setFlightNumber(b.getFlight().getFlightNumber());
        r.setFromCode(b.getFlight().getFromAirport().getCode());
        r.setFromCity(b.getFlight().getFromAirport().getCity());
        r.setToCode(b.getFlight().getToAirport().getCode());
        r.setToCity(b.getFlight().getToAirport().getCity());
        r.setDepartureTime(b.getFlight().getDepartureTime());
        r.setArrivalTime(b.getFlight().getArrivalTime());
        r.setSeatNumber(b.getSeat().getSeatNumber());
        r.setSeatClass(b.getSeat().getSeatClass().name());
        r.setPassengerName(b.getPassengerName());
        r.setPassengerAge(b.getPassengerAge() != null ? b.getPassengerAge().toString() : "");
        r.setPassengerGender(b.getPassengerGender());
        r.setCreatedAt(b.getCreatedAt());
        r.setAirlineName(b.getFlight().getAirlineName());
        return r;
    }
}
