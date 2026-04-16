package com.airline.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock Razorpay Payment Gateway Controller
 * Simulates Razorpay's real API endpoints:
 *   POST /api/razorpay/create-order  → returns order_id, amount, key
 *   POST /api/razorpay/verify        → simulates payment capture & signature verification
 */
@RestController
@RequestMapping("/api/razorpay")
public class RazorpayMockController {

    @Value("${razorpay.mock.key}")
    private String mockKey;

    @Value("${razorpay.mock.secret}")
    private String mockSecret;

    /**
     * Step 1 — Create a Razorpay order (called before showing checkout modal)
     * Real Razorpay: POST https://api.razorpay.com/v1/orders
     * Body: { amount, currency, receipt }
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> req) {
        try {
            // Amount comes in paise (₹1 = 100 paise), like real Razorpay
            Object amountObj = req.getOrDefault("amount", 0);
            long amount = ((Number) amountObj).longValue();
            String receipt = (String) req.getOrDefault("receipt", "rcpt_" + System.currentTimeMillis());

            String orderId = "order_" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 14).toUpperCase();

            Map<String, Object> order = new HashMap<>();
            order.put("id",       orderId);
            order.put("amount",   amount);
            order.put("currency", "INR");
            order.put("receipt",  receipt);
            order.put("status",   "created");
            order.put("key",      mockKey);

            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create order: " + e.getMessage()));
        }
    }

    /**
     * Step 2 — Verify payment after checkout modal completes
     * Real Razorpay: validates HMAC-SHA256 signature of orderId + "|" + paymentId
     * Mock: simulates capture with 95% success rate
     * Test payment_id "pay_FAIL000000000" always fails (for testing)
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> req) {
        String razorpayPaymentId = (String) req.getOrDefault("razorpay_payment_id", "");
        String razorpayOrderId   = (String) req.getOrDefault("razorpay_order_id",   "");

        // Simulate failure for test payment id
        if (razorpayPaymentId.contains("FAIL")) {
            Map<String, Object> fail = new HashMap<>();
            fail.put("verified", false);
            fail.put("error",    "Payment failed. Card declined.");
            fail.put("code",     "BAD_REQUEST_ERROR");
            return ResponseEntity.ok(fail);
        }

        // 95% success rate simulation
        boolean success = Math.random() > 0.05;

        // Generate mock signature (real Razorpay uses HMAC-SHA256)
        String mockSignature = "mock_sig_" + UUID.randomUUID().toString().replace("-","").substring(0, 20);

        Map<String, Object> result = new HashMap<>();
        result.put("verified",              success);
        result.put("razorpay_payment_id",   razorpayPaymentId);
        result.put("razorpay_order_id",     razorpayOrderId);
        result.put("razorpay_signature",    mockSignature);
        result.put("payment_status",        success ? "captured" : "failed");
        result.put("method",                req.getOrDefault("method", "card"));

        return ResponseEntity.ok(result);
    }
}
