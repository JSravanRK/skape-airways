package com.airline.controller;

import com.airline.dto.BookingDto;
import com.airline.service.BookingService;
import com.airline.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<?> createBooking(@RequestBody BookingDto.BookingRequest req,
                                           Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            return ResponseEntity.ok(bookingService.createBooking(userId, req));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> myBookings(Authentication auth) {
        Long userId = (Long) auth.getDetails();
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(bookingService.getBookingById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/ref/{ref}")
    public ResponseEntity<?> getByRef(@PathVariable String ref) {
        try {
            return ResponseEntity.ok(bookingService.getBookingByRef(ref));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            return ResponseEntity.ok(bookingService.cancelBooking(id, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    /**
     * Called once after all payments in a session succeed.
     * Sends a single combined payment receipt email listing all passengers.
     * Body: { bookingIds: [1,2,3], transactionId: "TXN...", paymentMethod: "RAZORPAY" }
     */
    @PostMapping("/send-payment-receipt")
    public ResponseEntity<?> sendPaymentReceipt(@RequestBody Map<String, Object> req,
                                                 Authentication auth) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> bookingIds = ((List<Object>) req.get("bookingIds"))
                    .stream()
                    .map(o -> Long.valueOf(o.toString()))
                    .collect(Collectors.toList());
            String transactionId  = req.get("transactionId").toString();
            String paymentMethod  = req.get("paymentMethod").toString();
            paymentService.sendGroupPaymentReceipt(bookingIds, transactionId, paymentMethod);
            return ResponseEntity.ok(Map.of("message", "Receipt sent."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
