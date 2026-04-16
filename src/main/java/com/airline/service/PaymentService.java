package com.airline.service;

import com.airline.dto.PaymentDto;
import com.airline.enums.BookingStatus;
import com.airline.enums.PaymentStatus;
import com.airline.model.Booking;
import com.airline.model.Payment;
import com.airline.repository.BookingRepository;
import com.airline.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService    bookingService;
    private final EmailService      emailService;
    private final MilesService      milesService;

    @Transactional
    public PaymentDto.PaymentResponse processPayment(PaymentDto.PaymentRequest req) {
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getStatus().name().equals("PENDING"))
            throw new RuntimeException("Booking is not in PENDING state. Cannot process payment.");

        // Apply voucher discount to the booking's stored total before charging
        double chargedAmount = booking.getTotalAmount();
        if (req.getOverrideAmount() > 0) {
            chargedAmount = req.getOverrideAmount();
        } else if (req.getDiscountAmount() > 0) {
            chargedAmount = Math.max(0, chargedAmount - req.getDiscountAmount());
        }
        booking.setTotalAmount(chargedAmount);
        bookingRepository.save(booking);

        String transactionId = "TXN" + UUID.randomUUID().toString()
                .replace("-","").substring(0,12).toUpperCase();

        boolean paymentSuccess = mockGatewayProcess(req);

        Optional<Payment> existingOpt = paymentRepository.findByBookingId(req.getBookingId());
        Payment payment;
        if (existingOpt.isPresent()) {
            payment = existingOpt.get();
            payment.setTransactionId(transactionId);
            payment.setPaymentMethod(req.getPaymentMethod());
            payment.setCardLastFour(extractLastFour(req));
            payment.setStatus(paymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            payment.setFailureReason(paymentSuccess ? null : "Payment declined by bank. Please try again.");
            payment.setAmount(chargedAmount);
        } else {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(chargedAmount)
                    .paymentMethod(req.getPaymentMethod())
                    .transactionId(transactionId)
                    .status(paymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                    .cardLastFour(extractLastFour(req))
                    .failureReason(paymentSuccess ? null : "Payment declined by bank. Please try again.")
                    .build();
        }
        paymentRepository.save(payment);

        PaymentDto.PaymentResponse resp = new PaymentDto.PaymentResponse();
        resp.setId(payment.getId());
        resp.setTransactionId(transactionId);
        resp.setAmount(booking.getTotalAmount());
        resp.setPaymentMethod(req.getPaymentMethod());
        resp.setBookingReference(booking.getBookingReference());

        if (paymentSuccess) {
            // Set CONFIRMED on the same instance we'll save for milesAwarded
            booking.setStatus(BookingStatus.CONFIRMED);
            // Award SkyMiles once per booking (idempotency guard)
            if (!booking.isMilesAwarded()) {
                int miles = (int) Math.round(chargedAmount / 10.0);
                String seatInfo = booking.getSeat().getSeatNumber()
                        + " (" + booking.getSeat().getSeatClass().name()
                            .replace("_", " ").toLowerCase() + ")";
                milesService.addMiles(booking.getUser().getId(), miles,
                        "Flight " + booking.getFlight().getFlightNumber() + " · "
                        + booking.getFlight().getFromAirport().getCity() + "→"
                        + booking.getFlight().getToAirport().getCity()
                        + " — Seat " + seatInfo);
                booking.setMilesAwarded(true);
            }
            bookingRepository.save(booking); // single save: CONFIRMED + milesAwarded
            resp.setStatus("SUCCESS");
            resp.setMessage("Payment successful! Your booking is confirmed.");
        } else {
            resp.setStatus("FAILED");
            resp.setMessage("Payment failed. Please try again with different details.");
        }
        return resp;
    }

    // For Razorpay mock flow
    @Transactional
    public PaymentDto.PaymentResponse processRazorpayPayment(Long bookingId,
                                                              String razorpayPaymentId,
                                                              String razorpayOrderId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getStatus().name().equals("PENDING"))
            throw new RuntimeException("Booking is not in PENDING state.");

        Optional<Payment> existingOpt = paymentRepository.findByBookingId(bookingId);
        Payment payment;
        if (existingOpt.isPresent()) {
            payment = existingOpt.get();
            payment.setTransactionId(razorpayPaymentId);
            payment.setPaymentMethod("RAZORPAY");
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setFailureReason(null);
        } else {
            payment = Payment.builder()
                    .booking(booking)
                    .amount(booking.getTotalAmount())
                    .paymentMethod("RAZORPAY")
                    .transactionId(razorpayPaymentId)
                    .status(PaymentStatus.SUCCESS)
                    .build();
        }
        paymentRepository.save(payment);
        // Set CONFIRMED on the same instance — avoids stale overwrite
        booking.setStatus(BookingStatus.CONFIRMED);
        // Award SkyMiles once per booking (idempotency guard)
        if (!booking.isMilesAwarded()) {
            int miles = (int) Math.round(booking.getTotalAmount() / 10.0);
            String seatInfo = booking.getSeat().getSeatNumber()
                    + " (" + booking.getSeat().getSeatClass().name()
                        .replace("_", " ").toLowerCase() + ")";
            milesService.addMiles(booking.getUser().getId(), miles,
                    "Flight " + booking.getFlight().getFlightNumber() + " · "
                    + booking.getFlight().getFromAirport().getCity() + "→"
                    + booking.getFlight().getToAirport().getCity()
                    + " — Seat " + seatInfo);
            booking.setMilesAwarded(true);
        }
        bookingRepository.save(booking); // single save: CONFIRMED + milesAwarded

        PaymentDto.PaymentResponse resp = new PaymentDto.PaymentResponse();
        resp.setId(payment.getId());
        resp.setTransactionId(razorpayPaymentId);
        resp.setAmount(booking.getTotalAmount());
        resp.setPaymentMethod("RAZORPAY");
        resp.setBookingReference(booking.getBookingReference());
        resp.setStatus("SUCCESS");
        resp.setMessage("Razorpay payment successful! Your booking is confirmed.");
        return resp;
    }

    /**
     * Sends a single combined payment receipt email for a group of bookings.
     * Called by BookingController after all payments in a session have been processed.
     */
    public void sendGroupPaymentReceipt(List<Long> bookingIds, String transactionId, String paymentMethod) {
        List<Booking> bookings = bookingRepository.findAllById(bookingIds);
        if (!bookings.isEmpty()) {
            emailService.sendPaymentReceipt(bookings, transactionId, paymentMethod);
        }
    }

    private boolean mockGatewayProcess(PaymentDto.PaymentRequest req) {
        // Explicit failure test values only — no random failures.
        // Random failures caused partial multi-booking confirmation (some PENDING, some CONFIRMED).
        if (req.getCardNumber() != null && req.getCardNumber().endsWith("0000")) return false;
        if (req.getUpiId() != null && req.getUpiId().equalsIgnoreCase("fail@upi")) return false;
        return true;
    }

    private String extractLastFour(PaymentDto.PaymentRequest req) {
        if (req.getCardNumber() != null && req.getCardNumber().length() >= 4)
            return req.getCardNumber().substring(req.getCardNumber().length() - 4);
        return null;
    }

    public PaymentDto.PaymentResponse getPaymentByBooking(Long bookingId) {
        Payment p = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("No payment found for booking " + bookingId));
        PaymentDto.PaymentResponse resp = new PaymentDto.PaymentResponse();
        resp.setId(p.getId());
        resp.setTransactionId(p.getTransactionId());
        resp.setAmount(p.getAmount());
        resp.setPaymentMethod(p.getPaymentMethod());
        resp.setStatus(p.getStatus().name());
        resp.setBookingReference(p.getBooking().getBookingReference());
        resp.setMessage(p.getStatus() == PaymentStatus.SUCCESS ? "Payment successful" : p.getFailureReason());
        return resp;
    }
}
