package com.gigshield.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
@Data
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    private AlertSeverity severity;

    private String description;
    private boolean resolved = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum AlertType {
        GPS_SPOOFING,
        ABNORMAL_SPEED,
        LOCATION_JUMP,
        NETWORK_GPS_MISMATCH,
        GROUP_FRAUD,
        DUPLICATE_CLAIM,
        SUSPICIOUS_TIMING,
        BEHAVIOR_ANOMALY
    }

    public enum AlertSeverity { LOW, MEDIUM, HIGH, CRITICAL }
}
