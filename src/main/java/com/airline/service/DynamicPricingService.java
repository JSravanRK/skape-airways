package com.airline.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Dynamic Pricing Engine
 * Price increases based on two factors:
 *  1. Demand (seat occupancy rate)  — up to 80% surcharge when nearly full
 *  2. Time urgency (days to depart) — up to 40% surcharge for last-minute
 */
@Service
public class DynamicPricingService {

    /**
     * Calculate dynamic price for a flight.
     *
     * @param basePrice      Original base price from admin
     * @param totalSeats     Total seats on the flight
     * @param availableSeats Remaining available seats
     * @param departureTime  Scheduled departure
     * @return               Dynamic price (rounded to nearest ₹50)
     */
    public double calculateDynamicPrice(double basePrice,
                                        int totalSeats,
                                        int availableSeats,
                                        LocalDateTime departureTime) {

        // ── Factor 1: Demand (occupancy rate) ────────────────────────────────
        double occupancyRate = (double)(totalSeats - availableSeats) / totalSeats;
        double demandMultiplier;
        if      (occupancyRate >= 0.90) demandMultiplier = 1.80;  // >90% full  → +80%
        else if (occupancyRate >= 0.75) demandMultiplier = 1.50;  // >75% full  → +50%
        else if (occupancyRate >= 0.60) demandMultiplier = 1.30;  // >60% full  → +30%
        else if (occupancyRate >= 0.40) demandMultiplier = 1.15;  // >40% full  → +15%
        else if (occupancyRate >= 0.20) demandMultiplier = 1.05;  // >20% full  → +5%
        else                            demandMultiplier = 1.00;  // low demand → base

        // ── Factor 2: Time urgency (days until departure) ────────────────────
        long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), departureTime);
        double timeMultiplier;
        if      (daysUntil <= 0)  timeMultiplier = 1.40;   // same/next day    → +40%
        else if (daysUntil <= 2)  timeMultiplier = 1.30;   // ≤2 days          → +30%
        else if (daysUntil <= 5)  timeMultiplier = 1.20;   // ≤5 days          → +20%
        else if (daysUntil <= 10) timeMultiplier = 1.12;   // ≤10 days         → +12%
        else if (daysUntil <= 20) timeMultiplier = 1.06;   // ≤20 days         → +6%
        else if (daysUntil <= 30) timeMultiplier = 1.02;   // ≤30 days         → +2%
        else                      timeMultiplier = 1.00;   // >30 days         → base

        double finalPrice = basePrice * demandMultiplier * timeMultiplier;

        // Round up to nearest ₹50 for clean pricing
        return Math.ceil(finalPrice / 50.0) * 50.0;
    }

    /**
     * Returns a human-readable demand label for UI display.
     */
    public String getDemandLabel(int totalSeats, int availableSeats) {
        double occupancyRate = (double)(totalSeats - availableSeats) / totalSeats;
        if      (occupancyRate >= 0.90) return "SELLING_FAST";
        else if (occupancyRate >= 0.75) return "HIGH_DEMAND";
        else if (occupancyRate >= 0.50) return "FILLING_UP";
        else if (occupancyRate >= 0.20) return "AVAILABLE";
        else                            return "LOW_DEMAND";
    }

    /**
     * Returns price change percent vs base price (for UI badge).
     */
    public int getPriceChangePercent(double basePrice, double dynamicPrice) {
        return (int) Math.round(((dynamicPrice - basePrice) / basePrice) * 100);
    }
}
