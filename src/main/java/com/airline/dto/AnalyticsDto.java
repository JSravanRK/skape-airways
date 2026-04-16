package com.airline.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AnalyticsDto {
    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private long pendingBookings;
    private double totalRevenue;
    private double revenueThisMonth;
    private long totalFlights;
    private long activeFlights;
    private long totalUsers;
    private long totalAirports;
    private List<MonthlyStats> monthlyStats;
    private List<RouteStats> popularRoutes;

    @Data
    public static class MonthlyStats {
        private String month;
        private long bookings;
        private double revenue;
    }

    @Data
    public static class RouteStats {
        private String from;
        private String to;
        private long count;
    }
}
