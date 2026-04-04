package com.gigshield.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "policies")
@Data
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private PlanType plan;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    private LocalDate startDate;
    private LocalDate expiryDate;

    private Double weeklyPremium;
    private Double coverageAmount;

    public enum PlanType { BASIC, STANDARD, PREMIUM }
    public enum PolicyStatus { ACTIVE, EXPIRED, CANCELLED }
}
