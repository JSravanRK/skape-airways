package com.airline.service;

import com.airline.dto.AnalyticsDto;
import com.airline.enums.BookingStatus;
import com.airline.enums.Role;
import com.airline.model.Booking;
import com.airline.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BookingRepository           bookingRepository;
    private final FlightRepository            flightRepository;
    private final UserRepository              userRepository;
    private final AirportRepository           airportRepository;
    private final PaymentRepository           paymentRepository;
    private final MilesTransactionRepository  milesTransactionRepository;
    private final VoucherRepository           voucherRepository;

    public AnalyticsDto getAnalytics() {
        AnalyticsDto dto = new AnalyticsDto();

        int year = LocalDateTime.now().getYear();

        dto.setTotalBookings(bookingRepository.count());
        dto.setConfirmedBookings(bookingRepository.countByStatus(BookingStatus.CONFIRMED));
        dto.setCancelledBookings(bookingRepository.countByStatus(BookingStatus.CANCELLED));
        dto.setPendingBookings(bookingRepository.countByStatus(BookingStatus.PENDING));
        dto.setTotalRevenue(bookingRepository.calculateTotalRevenue());

        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        dto.setRevenueThisMonth(bookingRepository.calculateRevenueBetween(startOfMonth, LocalDateTime.now()));

        dto.setTotalFlights(flightRepository.count());
        dto.setActiveFlights(flightRepository.count() - flightRepository.countCancelled());
        dto.setTotalUsers(userRepository.countByRole(Role.USER));
        dto.setTotalAirports(airportRepository.count());

        // Monthly stats
        List<Object[]> monthly = bookingRepository.findMonthlyStats(year);
        List<AnalyticsDto.MonthlyStats> monthlyList = new ArrayList<>();
        for (Object[] row : monthly) {
            AnalyticsDto.MonthlyStats ms = new AnalyticsDto.MonthlyStats();
            int monthNum = ((Number) row[0]).intValue();
            ms.setMonth(Month.of(monthNum).name().substring(0, 3));
            ms.setBookings(((Number) row[1]).longValue());
            ms.setRevenue(((Number) row[2]).doubleValue());
            monthlyList.add(ms);
        }
        dto.setMonthlyStats(monthlyList);

        // Popular routes
        List<Object[]> routes = flightRepository.findPopularRoutes();
        List<AnalyticsDto.RouteStats> routeList = new ArrayList<>();
        for (Object[] row : routes) {
            AnalyticsDto.RouteStats rs = new AnalyticsDto.RouteStats();
            rs.setFrom((String) row[0]);
            rs.setTo((String) row[1]);
            rs.setCount(((Number) row[2]).longValue());
            routeList.add(rs);
        }
        dto.setPopularRoutes(routeList);

        return dto;
    }

    /**
     * FIX: Delete user safely in FK-dependency order:
     *   payments → bookings → vouchers → miles_transactions → user
     * Previously only deleted payments + bookings, causing FK violations
     * when the user had miles transactions or vouchers.
     */
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found with id: " + userId);
        }

        // 1. Find all bookings for this user
        List<Booking> userBookings = bookingRepository.findByUserId(userId);

        // 2. Delete payments linked to those bookings
        for (Booking booking : userBookings) {
            paymentRepository.findByBookingId(booking.getId())
                    .ifPresent(paymentRepository::delete);
        }

        // 3. Delete all bookings
        bookingRepository.deleteAll(userBookings);

        // 4. Delete vouchers (FK: vouchers → users)
        voucherRepository.deleteAll(voucherRepository.findByUserIdOrderByCreatedAtDesc(userId));

        // 5. Delete miles transactions (FK: miles_transactions → users)
        milesTransactionRepository.deleteAll(
                milesTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId));

        // 6. Finally delete the user
        userRepository.deleteById(userId);
    }
}
