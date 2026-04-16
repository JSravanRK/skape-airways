package com.airline.repository;

import com.airline.model.MilesTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MilesTransactionRepository extends JpaRepository<MilesTransaction, Long> {
    List<MilesTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);
}
