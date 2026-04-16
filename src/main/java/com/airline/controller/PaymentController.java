package com.airline.controller;

import com.airline.dto.PaymentDto;
import com.airline.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // Traditional payment (Card / UPI / Net Banking)
    @PostMapping("/process")
    public ResponseEntity<?> processPayment(@RequestBody PaymentDto.PaymentRequest req,
                                            Authentication auth) {
        try {
            return ResponseEntity.ok(paymentService.processPayment(req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Razorpay mock payment — called after frontend verify succeeds
    @PostMapping("/razorpay/confirm")
    public ResponseEntity<?> confirmRazorpay(@RequestBody Map<String, Object> req,
                                              Authentication auth) {
        try {
            Long bookingId           = Long.valueOf(req.get("bookingId").toString());
            String razorpayPaymentId = (String) req.get("razorpayPaymentId");
            String razorpayOrderId   = (String) req.get("razorpayOrderId");
            return ResponseEntity.ok(
                paymentService.processRazorpayPayment(bookingId, razorpayPaymentId, razorpayOrderId)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<?> getByBooking(@PathVariable Long bookingId) {
        try {
            return ResponseEntity.ok(paymentService.getPaymentByBooking(bookingId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
