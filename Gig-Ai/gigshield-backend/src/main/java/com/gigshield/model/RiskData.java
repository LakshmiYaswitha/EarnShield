package com.gigshield.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_data")
@Data
public class RiskData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;
    private Double rainfall;
    private Double temperature;
    private Double aqi;

    @Enumerated(EnumType.STRING)
    private User.RiskLevel riskLevel;

    private LocalDateTime recordedAt = LocalDateTime.now();
}
