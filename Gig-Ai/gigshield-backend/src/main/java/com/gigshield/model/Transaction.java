package com.gigshield.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private Double amount;
    private String description;
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType { PREMIUM_DEBIT, PAYOUT_CREDIT, REFUND, PAYMENT_CREDIT }
}
