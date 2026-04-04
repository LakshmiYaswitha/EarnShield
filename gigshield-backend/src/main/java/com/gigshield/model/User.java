package com.gigshield.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String city;

    @Enumerated(EnumType.STRING)
    private Persona persona = Persona.FOOD_DELIVERY;

    private Double weeklyEarnings = 5000.0;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel = RiskLevel.LOW;

    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;

    private Double walletBalance = 0.0;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role { USER, ADMIN }
    public enum RiskLevel { LOW, MEDIUM, HIGH }
    public enum Persona { FOOD_DELIVERY, GROCERY_DELIVERY, ECOMMERCE_DELIVERY }
}
