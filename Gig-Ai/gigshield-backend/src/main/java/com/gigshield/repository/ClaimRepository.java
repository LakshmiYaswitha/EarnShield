package com.gigshield.repository;

import com.gigshield.model.Claim;
import com.gigshield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByUser(User user);
    List<Claim> findByUserAndClaimDateAfter(User user, LocalDateTime date);
    boolean existsByUserAndClaimDateAfterAndTrigger(User user, LocalDateTime date, Claim.ClaimTrigger trigger);
    List<Claim> findAllByOrderByClaimDateDesc();
}
