package com.gigshield.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
@Data
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private ClaimStatus status = ClaimStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_trigger")
    private ClaimTrigger trigger;

    private Double amount;
    private String triggerReason;
    private LocalDateTime claimDate = LocalDateTime.now();
    private boolean fraudFlag = false;

    public enum ClaimStatus { PENDING, APPROVED, REJECTED, PAID }
    public enum ClaimTrigger { RAIN, HEAT, AQI, MANUAL }
}
