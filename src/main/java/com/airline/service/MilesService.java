package com.airline.service;

import com.airline.dto.MilesDto;
import com.airline.model.MilesTransaction;
import com.airline.model.User;
import com.airline.model.Voucher;
import com.airline.repository.MilesTransactionRepository;
import com.airline.repository.UserRepository;
import com.airline.repository.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MilesService {

    private final UserRepository              userRepository;
    private final VoucherRepository           voucherRepository;
    private final MilesTransactionRepository  txRepository;

    // ── Balance + history ────────────────────────────────────────────────────

    public MilesDto.BalanceResponse getBalance(Long userId) {
        User user = getUser(userId);
        List<MilesTransaction> txList = txRepository.findByUserIdOrderByCreatedAtDesc(userId);

        MilesDto.BalanceResponse resp = new MilesDto.BalanceResponse();
        resp.setBalance(user.getMilesBalance());
        resp.setTransactions(txList.stream().map(tx -> {
            MilesDto.TransactionItem item = new MilesDto.TransactionItem();
            item.setAmount(tx.getAmount());
            item.setDescription(tx.getDescription());
            item.setCreatedAt(tx.getCreatedAt());
            return item;
        }).collect(Collectors.toList()));
        return resp;
    }

    // ── Earn miles (called after payment success) ────────────────────────────

    @Transactional
    public void addMiles(Long userId, int amount, String description) {
        User user = getUser(userId);
        int newBalance = user.getMilesBalance() + amount;
        // Never let balance go below zero
        if (newBalance < 0) newBalance = 0;
        user.setMilesBalance(newBalance);
        userRepository.save(user);

        MilesTransaction tx = MilesTransaction.builder()
                .user(user).amount(amount).description(description).build();
        txRepository.save(tx);
    }

    // ── Redeem miles → generate voucher ─────────────────────────────────────

    @Transactional
    public MilesDto.VoucherResponse redeemMiles(Long userId, String type) {
        User user = getUser(userId);

        int cost;
        String prefix;
        String label;
        switch (type) {
            case "FLIGHT_DISCOUNT" -> { cost = 500;  prefix = "SKYDIS"; label = "Flight Discount (₹50 off)"; }
            case "SEAT_UPGRADE"    -> { cost = 2000; prefix = "SKYUPG"; label = "Seat Upgrade (Economy→Business)"; }
            default -> throw new RuntimeException("Unknown voucher type: " + type);
        }

        if (user.getMilesBalance() < cost)
            throw new RuntimeException("Insufficient miles. Need " + cost + ", have " + user.getMilesBalance() + ".");

        // Deduct miles
        user.setMilesBalance(user.getMilesBalance() - cost);
        userRepository.save(user);

        // Log transaction
        MilesTransaction tx = MilesTransaction.builder()
                .user(user).amount(-cost).description("Redeemed: " + label).build();
        txRepository.save(tx);

        // Generate unique code
        String code = prefix + "-" + UUID.randomUUID().toString()
                .replace("-","").substring(0,6).toUpperCase();

        Voucher voucher = Voucher.builder()
                .user(user).code(code).type(type)
                .milesSpent(cost).used(false).build();
        voucherRepository.save(voucher);

        return toVoucherResponse(voucher);
    }

    // ── Get unused vouchers for user ─────────────────────────────────────────

    public List<MilesDto.VoucherResponse> getUserVouchers(Long userId) {
        return voucherRepository.findByUserIdAndUsedFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toVoucherResponse).collect(Collectors.toList());
    }

    public List<MilesDto.VoucherResponse> getAllUserVouchers(Long userId) {
        return voucherRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toVoucherResponse).collect(Collectors.toList());
    }

    // ── Validate a voucher code ──────────────────────────────────────────────

    public MilesDto.ValidateResponse validateVoucher(Long userId, String code) {
        MilesDto.ValidateResponse resp = new MilesDto.ValidateResponse();
        resp.setCode(code);

        Voucher voucher = voucherRepository.findByCode(code).orElse(null);
        if (voucher == null) {
            resp.setValid(false); resp.setMessage("Voucher code not found."); return resp;
        }
        if (!voucher.getUser().getId().equals(userId)) {
            resp.setValid(false); resp.setMessage("This voucher does not belong to your account."); return resp;
        }
        if (voucher.isUsed()) {
            resp.setValid(false); resp.setMessage("This voucher has already been used."); return resp;
        }

        resp.setValid(true);
        resp.setType(voucher.getType());
        resp.setDiscountAmount(voucher.getType().equals("FLIGHT_DISCOUNT") ? 50 : 0);
        resp.setMessage("Voucher applied successfully!");
        return resp;
    }

    // ── Mark voucher used (called after payment) ──────────────────────────────

    @Transactional
    public void markVoucherUsed(String code) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Voucher not found: " + code));
        if (voucher.isUsed())
            throw new RuntimeException("Voucher already used.");
        voucher.setUsed(true);
        voucher.setUsedAt(LocalDateTime.now());
        voucherRepository.save(voucher);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private MilesDto.VoucherResponse toVoucherResponse(Voucher v) {
        MilesDto.VoucherResponse r = new MilesDto.VoucherResponse();
        r.setId(v.getId());
        r.setCode(v.getCode());
        r.setType(v.getType());
        r.setMilesSpent(v.getMilesSpent());
        r.setUsed(v.isUsed());
        r.setCreatedAt(v.getCreatedAt());
        r.setUsedAt(v.getUsedAt());
        return r;
    }
}
