package com.airline.dto;

import lombok.Data;

public class PaymentDto {

    @Data
    public static class PaymentRequest {
        private Long bookingId;
        private String paymentMethod;   // CREDIT_CARD, DEBIT_CARD, UPI, NETBANKING
        private String cardNumber;
        private String cardExpiry;
        private String cardCvv;
        private String cardHolderName;
        private String upiId;
        /** Optional: ₹ discount from a FLIGHT_DISCOUNT voucher */
        private double discountAmount;
        /** Optional: override total amount charged (for SEAT_UPGRADE voucher) */
        private double overrideAmount;
    }

    @Data
    public static class PaymentResponse {
        private Long id;
        private String transactionId;
        private String status;
        private double amount;
        private String paymentMethod;
        private String bookingReference;
        private String message;
    }
}
