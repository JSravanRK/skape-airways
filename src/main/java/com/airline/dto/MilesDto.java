package com.airline.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

public class MilesDto {

    @Data
    public static class BalanceResponse {
        private int balance;
        private List<TransactionItem> transactions;
    }

    @Data
    public static class TransactionItem {
        private int amount;
        private String description;
        private LocalDateTime createdAt;
    }

    @Data
    public static class RedeemRequest {
        /** FLIGHT_DISCOUNT or SEAT_UPGRADE */
        private String type;
    }

    @Data
    public static class VoucherResponse {
        private Long id;
        private String code;
        private String type;
        private int milesSpent;
        private boolean used;
        private LocalDateTime createdAt;
        private LocalDateTime usedAt;
    }

    @Data
    public static class ValidateResponse {
        private boolean valid;
        private String code;
        private String type;
        private String message;
        /** For FLIGHT_DISCOUNT: 50. For SEAT_UPGRADE: used differently. */
        private int discountAmount;
    }
}
