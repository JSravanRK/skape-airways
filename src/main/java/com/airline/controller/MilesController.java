package com.airline.controller;

import com.airline.dto.MilesDto;
import com.airline.service.MilesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/miles")
@RequiredArgsConstructor
public class MilesController {

    private final MilesService milesService;

    /** GET /api/miles/balance — balance + transaction history */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            return ResponseEntity.ok(milesService.getBalance(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/miles/vouchers — unused vouchers for current user */
    @GetMapping("/vouchers")
    public ResponseEntity<?> getVouchers(Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            return ResponseEntity.ok(milesService.getUserVouchers(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/miles/vouchers/all — all vouchers (including used) */
    @GetMapping("/vouchers/all")
    public ResponseEntity<?> getAllVouchers(Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            return ResponseEntity.ok(milesService.getAllUserVouchers(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/miles/redeem — redeem miles for a voucher
     *  Body: { "type": "FLIGHT_DISCOUNT" | "SEAT_UPGRADE" }
     */
    @PostMapping("/redeem")
    public ResponseEntity<?> redeem(@RequestBody MilesDto.RedeemRequest req,
                                     Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            MilesDto.VoucherResponse voucher = milesService.redeemMiles(userId, req.getType());
            return ResponseEntity.ok(voucher);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/miles/vouchers/validate — check a code before applying
     *  Body: { "code": "SKYDIS-XXXXXX" }
     */
    @PostMapping("/vouchers/validate")
    public ResponseEntity<?> validate(@RequestBody Map<String, String> req,
                                       Authentication auth) {
        try {
            Long userId = (Long) auth.getDetails();
            String code = req.get("code");
            return ResponseEntity.ok(milesService.validateVoucher(userId, code));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/miles/vouchers/use — mark voucher as used after payment
     *  Body: { "code": "SKYDIS-XXXXXX" }
     */
    @PostMapping("/vouchers/use")
    public ResponseEntity<?> use(@RequestBody Map<String, String> req,
                                  Authentication auth) {
        try {
            String code = req.get("code");
            milesService.markVoucherUsed(code);
            return ResponseEntity.ok(Map.of("message", "Voucher marked as used."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
