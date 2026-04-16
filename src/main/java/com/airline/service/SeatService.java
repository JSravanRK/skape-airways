package com.airline.service;

import com.airline.model.Seat;
import com.airline.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;

    @Value("${seat.lock.duration.minutes}")
    private int lockDurationMinutes;

    // Called for unauthenticated / public seat view — no userId available
    public List<Map<String, Object>> getSeatMap(Long flightId) {
        List<Seat> seats = seatRepository.findByFlightIdOrderBySeatNumber(flightId);
        LocalDateTime now = LocalDateTime.now();
        return seats.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",         s.getId());
            m.put("seatNumber", s.getSeatNumber());
            m.put("seatClass",  s.getSeatClass().name());
            m.put("booked",     s.isBooked());
            m.put("locked",     s.isLocked() && s.getLockExpiresAt() != null
                                && s.getLockExpiresAt().isAfter(now));
            m.put("lockedByMe", false);   // no userId in this context — always false
            return m;
        }).collect(Collectors.toList());
    }

    // Called for authenticated users — knows whose lock belongs to whom
    public List<Map<String, Object>> getSeatMapForUser(Long flightId, Long userId) {
        List<Seat> seats = seatRepository.findByFlightIdOrderBySeatNumber(flightId);
        LocalDateTime now = LocalDateTime.now();
        return seats.stream().map(s -> {
            boolean effectiveLocked = s.isLocked() && s.getLockExpiresAt() != null
                                      && s.getLockExpiresAt().isAfter(now);
            boolean lockedByMe      = effectiveLocked && userId.equals(s.getLockedByUserId());
            Map<String, Object> m = new HashMap<>();
            m.put("id",         s.getId());
            m.put("seatNumber", s.getSeatNumber());
            m.put("seatClass",  s.getSeatClass().name());
            m.put("booked",     s.isBooked());
            m.put("locked",     effectiveLocked && !lockedByMe);
            m.put("lockedByMe", lockedByMe);
            return m;
        }).collect(Collectors.toList());
    }

    /**
     * Locks a seat for a user using PESSIMISTIC_WRITE DB lock to prevent race conditions.
     * FIX: Removed the blanket releaseUserLocks(userId) call that was wiping out
     * ALL previously selected seats whenever the user picked a new seat.
     * Now we only release the existing lock on THIS specific seat if it was held
     * by another user (or expired), and leave other seats the user has locked alone.
     */
    @Transactional
    public Map<String, Object> lockSeat(Long seatId, Long userId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        LocalDateTime now = LocalDateTime.now();

        if (seat.isBooked()) {
            throw new RuntimeException("Seat is already booked.");
        }
        if (seat.isLocked() && seat.getLockExpiresAt() != null
                && seat.getLockExpiresAt().isAfter(now)
                && !userId.equals(seat.getLockedByUserId())) {
            throw new RuntimeException("Seat is temporarily held by another user. Please try a different seat.");
        }

        // FIX: Do NOT call releaseUserLocks(userId) here — that was releasing ALL
        // other seats the user had already selected, breaking multi-seat booking.
        // Instead, just (re-)lock this specific seat for the user.

        // Apply new lock
        seat.setLocked(true);
        seat.setLockedByUserId(userId);
        seat.setLockExpiresAt(now.plusMinutes(lockDurationMinutes));
        seatRepository.save(seat);

        Map<String, Object> result = new HashMap<>();
        result.put("seatId",              seat.getId());
        result.put("seatNumber",          seat.getSeatNumber());
        result.put("seatClass",           seat.getSeatClass().name());
        result.put("lockExpiresAt",       seat.getLockExpiresAt().toString());
        result.put("lockDurationMinutes", lockDurationMinutes);
        return result;
    }

    @Transactional
    public void releaseLock(Long seatId, Long userId) {
        Seat seat = seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new RuntimeException("Seat not found"));
        if (userId.equals(seat.getLockedByUserId()) && !seat.isBooked()) {
            seat.setLocked(false);
            seat.setLockedByUserId(null);
            seat.setLockExpiresAt(null);
            seatRepository.save(seat);
        }
    }
}
