package com.airline.scheduler;

import com.airline.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SeatLockScheduler {

    private final SeatRepository seatRepository;

    /**
     * Runs every 60 seconds to release expired seat locks.
     * This is critical for concurrency — prevents seats from being permanently locked
     * if a user abandons checkout.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void releaseExpiredLocks() {
        int released = seatRepository.releaseExpiredLocks(LocalDateTime.now());
        if (released > 0) {
            
        }
    }
}
