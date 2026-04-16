package com.airline.service;

import com.airline.model.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Email Confirmation Service
 * Sends HTML emails for booking confirmation, payment receipt, and cancellation.
 * If email is disabled or SMTP fails, errors are logged and app continues normally.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── Booking Confirmation (single) ────────────────────────────────────────
    public void sendBookingConfirmation(Booking booking) {
        sendBookingConfirmation(List.of(booking));
    }

    // ── Booking Confirmation (multiple seats) ─────────────────────────────────
    public void sendBookingConfirmation(List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) return;
        Booking first = bookings.get(0);
        if (!emailEnabled) {
            log.info("📧 [EMAIL SIMULATION] Booking confirmation for {} → {}",
                    first.getPassengerName(), first.getUser().getEmail());
            bookings.forEach(b -> log.info("   Ref: {} | Seat: {} | ₹{}",
                    b.getBookingReference(), b.getSeat().getSeatNumber(), b.getTotalAmount()));
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(first.getUser().getEmail());
            String subject = bookings.size() == 1
                ? "✈ Booking Confirmed — " + first.getBookingReference() + " | Skape Airways"
                : "✈ " + bookings.size() + " Seats Confirmed — " + first.getBookingReference() + " | Skape Airways";
            helper.setSubject(subject);
            helper.setText(buildBookingEmailHtml(bookings), true);
            mailSender.send(msg);
            log.info("📧 Booking confirmation email sent to {}", first.getUser().getEmail());
        } catch (Exception e) {
            log.error("📧 Failed to send booking confirmation email: {}", e.getMessage());
        }
    }

    // ── Payment Receipt (single) ─────────────────────────────────────────────
    public void sendPaymentReceipt(Booking booking, String transactionId, String paymentMethod) {
        sendPaymentReceipt(List.of(booking), transactionId, paymentMethod);
    }

    // ── Payment Receipt (multiple seats) ─────────────────────────────────────
    public void sendPaymentReceipt(List<Booking> bookings, String transactionId, String paymentMethod) {
        if (bookings == null || bookings.isEmpty()) return;
        Booking first = bookings.get(0);
        if (!emailEnabled) {
            log.info("📧 [EMAIL SIMULATION] Payment receipt for {} | TXN: {} | seats: {}",
                    first.getUser().getEmail(), transactionId, bookings.size());
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(first.getUser().getEmail());
            helper.setSubject("💳 Payment Successful — " + first.getBookingReference() + " | Skape Airways");
            helper.setText(buildPaymentEmailHtml(bookings, transactionId, paymentMethod), true);
            mailSender.send(msg);
            log.info("📧 Payment receipt sent to {}", first.getUser().getEmail());
        } catch (Exception e) {
            log.error("📧 Failed to send payment receipt: {}", e.getMessage());
        }
    }

    // ── Cancellation ──────────────────────────────────────────────────────────
    public void sendCancellationEmail(Booking booking) {
        if (!emailEnabled) {
            log.info("📧 [EMAIL SIMULATION] Cancellation email for {} | Ref: {}",
                    booking.getUser().getEmail(), booking.getBookingReference());
            return;
        }
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("❌ Booking Cancelled — " + booking.getBookingReference() + " | Skape Airways");
            helper.setText(buildCancellationEmailHtml(booking), true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("📧 Failed to send cancellation email: {}", e.getMessage());
        }
    }

    // ── HTML Templates ────────────────────────────────────────────────────────

    private String buildBookingEmailHtml(Booking b) {
        return buildBookingEmailHtml(List.of(b));
    }

    private String buildBookingEmailHtml(List<Booking> bookings) {
        Booking first = bookings.get(0);
        String passengerSection = bookings.size() == 1
            ? passengersTable(bookings)
            : "<p style='color:#64748b;font-size:14px;margin-bottom:8px'><strong>" + bookings.size() + " passengers</strong> booked on this flight.</p>" + passengersTable(bookings);
        return emailBase(
            "✈ Booking Confirmed!",
            "#10b981",
            "<h2 style='color:#10b981;margin:0 0 8px'>Your booking is confirmed</h2>" +
            "<p style='color:#64748b;margin:0'>Booking Reference: <strong style='font-family:monospace;font-size:18px;color:#1e293b'>"
                + first.getBookingReference() + "</strong></p>",
            flightInfoBlock(first) + passengerSection,
            "<p style='color:#64748b;font-size:14px;text-align:center;margin-top:24px'>" +
            "Payment is pending. Please complete payment to confirm your seat.</p>"
        );
    }

    private String buildPaymentEmailHtml(Booking b, String txnId, String method) {
        return buildPaymentEmailHtml(List.of(b), txnId, method);
    }

    private String buildPaymentEmailHtml(List<Booking> bookings, String txnId, String method) {
        Booking first = bookings.get(0);
        double totalAmount = bookings.stream().mapToDouble(Booking::getTotalAmount).sum();
        return emailBase(
            "💳 Payment Successful!",
            "#1a56db",
            "<h2 style='color:#1a56db;margin:0 0 8px'>Payment received — you're all set!</h2>" +
            "<p style='color:#64748b;margin:0'>Transaction ID: <strong style='font-family:monospace;color:#1e293b'>"
                + txnId + "</strong></p>",
            flightInfoBlock(first) +
            passengersTable(bookings) +
            boardingPassBlock(bookings) +
            "<table style='width:100%;border-collapse:collapse;margin-top:16px'>" +
            "<tr><td style='padding:8px;color:#64748b;border-bottom:1px solid #e2e8f0'>Payment Method</td><td style='padding:8px;font-weight:600;border-bottom:1px solid #e2e8f0'>" + method + "</td></tr>" +
            "<tr><td style='padding:8px;color:#64748b'>Total Amount Paid</td><td style='padding:8px;font-weight:700;color:#10b981;font-size:18px'>₹" + String.format("%.0f", totalAmount) + "</td></tr>" +
            "</table>",
            "<p style='color:#64748b;font-size:13px;text-align:center;margin-top:24px'>" +
            "Please carry a printout of your e-ticket or show your booking reference at the airport.</p>"
        );
    }

    private String buildCancellationEmailHtml(Booking b) {
        return emailBase(
            "❌ Booking Cancelled",
            "#ef4444",
            "<h2 style='color:#ef4444;margin:0 0 8px'>Your booking has been cancelled</h2>" +
            "<p style='color:#64748b;margin:0'>Booking Reference: <strong style='font-family:monospace;color:#1e293b'>"
                + b.getBookingReference() + "</strong></p>",
            flightCard(b),
            "<p style='color:#64748b;font-size:14px;text-align:center;margin-top:24px'>" +
            "Refund will be processed within 5-7 business days to your original payment method.</p>"
        );
    }

    // Flight info block (no passenger rows — those are in passengersTable)
    private String flightInfoBlock(Booking b) {
        String dep = b.getFlight().getDepartureTime() != null ? b.getFlight().getDepartureTime().format(FMT) : "";
        String arr = b.getFlight().getArrivalTime()   != null ? b.getFlight().getArrivalTime().format(FMT)   : "";
        return "<div style='background:#f8fafc;border-radius:12px;padding:20px;margin:20px 0;border:1px solid #e2e8f0'>" +
            "<div style='display:flex;justify-content:space-between;align-items:center;margin-bottom:16px'>" +
            "<span style='font-size:13px;color:#64748b'>✈ " + (b.getFlight().getAirlineName() != null ? b.getFlight().getAirlineName() : "Skape Airways") + " · " + b.getFlight().getFlightNumber() + "</span>" +
            "</div>" +
            "<table style='width:100%;border-collapse:collapse'>" +
            "<tr>" +
            "<td style='text-align:left'><div style='font-size:28px;font-weight:700'>" + b.getFlight().getFromAirport().getCode() + "</div><div style='color:#64748b'>" + b.getFlight().getFromAirport().getCity() + "</div><div style='font-size:13px;color:#94a3b8'>" + dep + "</div></td>" +
            "<td style='text-align:center;padding:0 16px'><div style='font-size:20px'>✈</div><div style='height:2px;background:#e2e8f0;margin:6px 0'></div><div style='font-size:12px;color:#64748b'>Non-stop</div></td>" +
            "<td style='text-align:right'><div style='font-size:28px;font-weight:700'>" + b.getFlight().getToAirport().getCode() + "</div><div style='color:#64748b'>" + b.getFlight().getToAirport().getCity() + "</div><div style='font-size:13px;color:#94a3b8'>" + arr + "</div></td>" +
            "</tr></table>" +
            "</div>";
    }

    // Passengers table — lists every passenger with their seat number
    private String passengersTable(List<Booking> bookings) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < bookings.size(); i++) {
            Booking b = bookings.get(i);
            String bg = (i % 2 == 0) ? "#ffffff" : "#f8fafc";
            rows.append("<tr style='background:").append(bg).append("'>")
                .append("<td style='padding:10px 12px;border-bottom:1px solid #e2e8f0'>").append(i + 1).append("</td>")
                .append("<td style='padding:10px 12px;border-bottom:1px solid #e2e8f0;font-weight:600'>").append(b.getPassengerName()).append("</td>")
                .append("<td style='padding:10px 12px;border-bottom:1px solid #e2e8f0'>").append(b.getSeat().getSeatNumber()).append("</td>")
                .append("<td style='padding:10px 12px;border-bottom:1px solid #e2e8f0;color:#64748b'>").append(b.getSeat().getSeatClass()).append("</td>")
                .append("<td style='padding:10px 12px;border-bottom:1px solid #e2e8f0;font-family:monospace;font-size:12px;color:#475569'>").append(b.getBookingReference()).append("</td>")
                .append("</tr>");
        }
        return "<div style='margin:16px 0'>" +
            "<div style='font-size:13px;font-weight:700;color:#1e293b;text-transform:uppercase;letter-spacing:.04em;margin-bottom:8px'>Passengers &amp; Seats</div>" +
            "<table style='width:100%;border-collapse:collapse;border:1px solid #e2e8f0;border-radius:8px;overflow:hidden'>" +
            "<thead><tr style='background:#0f2b6e;color:#fff'>" +
            "<th style='padding:10px 12px;text-align:left;font-size:12px'>#</th>" +
            "<th style='padding:10px 12px;text-align:left;font-size:12px'>Passenger Name</th>" +
            "<th style='padding:10px 12px;text-align:left;font-size:12px'>Seat</th>" +
            "<th style='padding:10px 12px;text-align:left;font-size:12px'>Class</th>" +
            "<th style='padding:10px 12px;text-align:left;font-size:12px'>Reference</th>" +
            "</tr></thead>" +
            "<tbody>" + rows + "</tbody>" +
            "</table></div>";
    }

    // Keep old flightCard for cancellation emails (single booking)
    private String flightCard(Booking b) {
        return flightInfoBlock(b) + passengersTable(List.of(b));
    }

    // ── Barcode SVG generator ──────────────────────────────────────────────
    // Generates a barcode as an HTML <table> — works in ALL email clients.
    // SVG is stripped by Gmail/Outlook, so we use table cells for bars instead.
    private String generateBarcodeHtml(String text) {
        int seed = 0;
        for (char c : text.toCharArray()) seed += c;

        StringBuilder sb = new StringBuilder();
        sb.append("<table cellpadding='0' cellspacing='0' border='0' ")
          .append("style='border-collapse:collapse;display:inline-table'>")
          .append("<tr>");

        for (int i = 0; i < 60; i++) {
            int w   = ((seed * (i + 7) * 13) % 3) + 1;
            int gap = (i % 5 == 0) ? 2 : 1;
            // Black bar
            sb.append("<td style='background:#1a1f2e;width:").append(w)
              .append("px;height:48px;padding:0;line-height:0'></td>");
            // White gap
            sb.append("<td style='background:#ffffff;width:").append(gap)
              .append("px;height:48px;padding:0;line-height:0'></td>");
        }

        sb.append("</tr></table>")
          .append("<div style='font-family:\"Courier New\",monospace;font-size:9px;")
          .append("color:#94a3b8;letter-spacing:3px;text-align:center;")
          .append("margin-top:5px;text-transform:uppercase'>")
          .append(text)
          .append("</div>");

        return sb.toString();
    }

    // ── Boarding pass card (one per booking, appended to payment receipt) ───
    private String boardingPassBlock(List<Booking> bookings) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='margin-top:28px'>");
        html.append("<div style='font-size:13px;font-weight:700;color:#1e293b;")
            .append("text-transform:uppercase;letter-spacing:.04em;margin-bottom:14px'>")
            .append("&#127903; Boarding Pass</div>");

        for (Booking b : bookings) {
            String barcodeHtml = generateBarcodeHtml(b.getBookingReference());
            String dep = b.getFlight().getDepartureTime() != null
                    ? b.getFlight().getDepartureTime().format(FMT) : "";

            // Card wrapper (dark navy)
            html.append("<div style='background:#0f2b6e;border-radius:12px;overflow:hidden;")
                .append("margin-bottom:20px;font-family:Segoe UI,sans-serif'>");

            // Top header
            html.append("<div style='padding:14px 20px;display:flex;justify-content:space-between;")
                .append("align-items:center;border-bottom:1px dashed rgba(255,255,255,.15)'>");
            html.append("<span style='color:rgba(255,255,255,.55);font-size:11px;")
                .append("text-transform:uppercase;letter-spacing:.12em;font-weight:700'>")
                .append("&#10022; SKAPE AIRWAYS</span>");
            html.append("<span style='color:rgba(255,255,255,.4);font-size:11px'>")
                .append("Electronic Boarding Pass</span>");
            html.append("</div>");

            // Route
            html.append("<div style='padding:20px;display:flex;justify-content:space-between;")
                .append("align-items:center'>");
            html.append("<div><div style='font-size:34px;font-weight:700;color:#fff;line-height:1'>")
                .append(b.getFlight().getFromAirport().getCode()).append("</div>")
                .append("<div style='font-size:12px;color:rgba(255,255,255,.5);margin-top:4px'>")
                .append(b.getFlight().getFromAirport().getCity()).append("</div></div>");
            html.append("<div style='text-align:center;flex:1;padding:0 16px'>")
                .append("<div style='font-size:22px;color:#c9a84c'>&#9992;</div>")
                .append("<div style='height:1px;background:rgba(201,168,76,.35);margin:6px 0'></div>")
                .append("<div style='font-size:11px;color:rgba(255,255,255,.3)'>NON-STOP</div>")
                .append("</div>");
            html.append("<div style='text-align:right'>")
                .append("<div style='font-size:34px;font-weight:700;color:#fff;line-height:1'>")
                .append(b.getFlight().getToAirport().getCode()).append("</div>")
                .append("<div style='font-size:12px;color:rgba(255,255,255,.5);margin-top:4px'>")
                .append(b.getFlight().getToAirport().getCity()).append("</div></div>");
            html.append("</div>");

            // Passenger details grid
            html.append("<div style='padding:0 20px 18px;display:flex;gap:20px;flex-wrap:wrap'>");
            html.append(bpField("PASSENGER",  b.getPassengerName()));
            html.append(bpField("SEAT",       b.getSeat().getSeatNumber() + " \u00B7 " + b.getSeat().getSeatClass()));
            html.append(bpField("FLIGHT",     b.getFlight().getFlightNumber()));
            html.append(bpField("DEPARTURE",  dep));
            html.append(bpField("BOOKING REF", b.getBookingReference()));
            html.append("</div>");

            // Barcode strip (white background section)
            html.append("<div style='background:#ffffff;padding:16px 20px 12px;text-align:center;")
                .append("border-top:2px dashed rgba(15,43,110,.12)'>");
            html.append("<div style='display:inline-block;padding:10px 18px;background:#f8fafc;")
                .append("border-radius:6px;border:1px solid #e2e8f0;line-height:0'>")
                .append(barcodeHtml)
                .append("</div>");
            html.append("<div style='margin-top:8px;font-size:10px;color:#94a3b8;")
                .append("letter-spacing:.12em;text-transform:uppercase'>SCAN AT GATE</div>");
            html.append("</div>");

            html.append("</div>"); // end card
        }
        html.append("</div>"); // end wrapper
        return html.toString();
    }

    private String bpField(String label, String value) {
        return "<div style='min-width:100px'>"
             + "<div style='font-size:10px;color:rgba(255,255,255,.4);text-transform:uppercase;"
             + "letter-spacing:.08em;margin-bottom:3px'>" + label + "</div>"
             + "<div style='font-size:13px;font-weight:600;color:#fff'>" + value + "</div>"
             + "</div>";
    }

        private String emailBase(String title, String color, String header, String body, String footer) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/></head><body style='font-family:Segoe UI,sans-serif;background:#f0f4f8;margin:0;padding:24px'>" +
            "<div style='max-width:600px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,.08)'>" +
            "<div style='background:linear-gradient(135deg,#0f2b6e," + color + ");padding:32px 32px 24px;color:#fff'>" +
            "<div style='font-size:22px;font-weight:700;margin-bottom:4px'>✈ Skape Airways</div>" +
            "<div style='font-size:13px;opacity:.8'>" + title + "</div>" +
            "</div>" +
            "<div style='padding:32px'>" + header + body + footer + "</div>" +
            "<div style='background:#f8fafc;padding:20px 32px;text-align:center;font-size:12px;color:#94a3b8;border-top:1px solid #e2e8f0'>" +
            "© 2026 Skape Airways. All rights reserved.<br/>This is an automated email, please do not reply." +
            "</div></div></body></html>";
    }
}
