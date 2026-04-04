package com.gigshield.repository;

import com.gigshield.model.UserActivity;
import com.gigshield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {
    List<UserActivity> findByUserAndRecordedAtAfterOrderByRecordedAtDesc(User user, LocalDateTime after);
    Optional<UserActivity> findTopByUserOrderByRecordedAtDesc(User user);
}
