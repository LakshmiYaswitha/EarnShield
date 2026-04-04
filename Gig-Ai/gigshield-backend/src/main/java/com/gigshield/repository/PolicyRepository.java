package com.gigshield.repository;

import com.gigshield.model.Policy;
import com.gigshield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    Optional<Policy> findByUserAndStatus(User user, Policy.PolicyStatus status);
    List<Policy> findByUser(User user);
}
