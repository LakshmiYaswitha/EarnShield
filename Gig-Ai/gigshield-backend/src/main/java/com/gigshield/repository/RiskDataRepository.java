package com.gigshield.repository;

import com.gigshield.model.RiskData;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RiskDataRepository extends JpaRepository<RiskData, Long> {
    Optional<RiskData> findTopByCityOrderByRecordedAtDesc(String city);
}
