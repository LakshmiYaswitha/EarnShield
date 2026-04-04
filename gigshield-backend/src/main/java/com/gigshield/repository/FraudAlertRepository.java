package com.gigshield.repository;

import com.gigshield.model.FraudAlert;
import com.gigshield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {
    List<FraudAlert> findByUserOrderByCreatedAtDesc(User user);
    List<FraudAlert> findByResolvedFalseOrderByCreatedAtDesc();
    List<FraudAlert> findAllByOrderByCreatedAtDesc();
    long countByUserAndCreatedAtAfter(User user, LocalDateTime after);

    @Query("SELECT f.user.city, COUNT(f) FROM FraudAlert f WHERE f.createdAt > :after GROUP BY f.user.city HAVING COUNT(f) > :threshold")
    List<Object[]> findCitiesWithGroupFraud(LocalDateTime after, long threshold);
}
