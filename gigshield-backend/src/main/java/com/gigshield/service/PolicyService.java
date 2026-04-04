package com.gigshield.service;

import com.gigshield.model.*;
import com.gigshield.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    private static final Map<Policy.PlanType, double[]> PLAN_CONFIG = Map.of(
        Policy.PlanType.BASIC,    new double[]{49.0,  500.0},
        Policy.PlanType.STANDARD, new double[]{99.0,  1500.0},
        Policy.PlanType.PREMIUM,  new double[]{199.0, 3000.0}
    );

    private double getRiskMultiplier(User.RiskLevel risk) {
        return switch (risk) {
            case HIGH   -> 1.5;
            case MEDIUM -> 1.2;
            default     -> 1.0;
        };
    }

    private double getPersonaMultiplier(User.Persona persona) {
        if (persona == null) return 1.0;
        return switch (persona) {
            case ECOMMERCE_DELIVERY -> 1.1;
            case GROCERY_DELIVERY   -> 1.05;
            default                 -> 1.0;
        };
    }

    public double calculateDynamicPremium(User user, Policy.PlanType plan) {
        double base = PLAN_CONFIG.get(plan)[0];
        double dynamic = base * getRiskMultiplier(user.getRiskLevel()) * getPersonaMultiplier(user.getPersona());
        return Math.round(dynamic * 100.0) / 100.0;
    }

    public Policy activatePolicy(User user, String planName) {
        Policy.PlanType plan = Policy.PlanType.valueOf(planName.toUpperCase());

        // HIGH risk users can only select PREMIUM
        if (user.getRiskLevel() == User.RiskLevel.HIGH && plan != Policy.PlanType.PREMIUM)
            throw new RuntimeException("High-risk users must select the Premium plan");

        // Cancel existing active policy
        policyRepository.findByUserAndStatus(user, Policy.PolicyStatus.ACTIVE)
                .ifPresent(p -> { p.setStatus(Policy.PolicyStatus.CANCELLED); policyRepository.save(p); });

        double[] config = PLAN_CONFIG.get(plan);
        Policy policy = new Policy();
        policy.setUser(user);
        policy.setPlan(plan);
        policy.setStartDate(LocalDate.now());
        policy.setExpiryDate(LocalDate.now().plusDays(30));
        policy.setWeeklyPremium(config[0]);
        policy.setCoverageAmount(config[1]);
        policyRepository.save(policy);

        // Deduct first week premium
        deductPremium(user, policy);
        return policy;
    }

    public void deductPremium(User user, Policy policy) {
        if (user.getWalletBalance() < policy.getWeeklyPremium())
            throw new RuntimeException("Insufficient balance. Top up ₹"
                + String.format("%.0f", policy.getWeeklyPremium() - user.getWalletBalance()) + " more to activate.");
        user.setWalletBalance(user.getWalletBalance() - policy.getWeeklyPremium());
        userRepository.save(user);

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setType(Transaction.TransactionType.PREMIUM_DEBIT);
        tx.setAmount(policy.getWeeklyPremium());
        tx.setDescription("Weekly premium for " + policy.getPlan() + " plan");
        transactionRepository.save(tx);
    }

    public Policy payAndActivate(User user, String planName, String paymentMethod) {
        Policy.PlanType plan = Policy.PlanType.valueOf(planName.toUpperCase());
        if (user.getRiskLevel() == User.RiskLevel.HIGH && plan != Policy.PlanType.PREMIUM)
            throw new RuntimeException("High-risk users must select the Premium plan");

        policyRepository.findByUserAndStatus(user, Policy.PolicyStatus.ACTIVE)
                .ifPresent(p -> { p.setStatus(Policy.PolicyStatus.CANCELLED); policyRepository.save(p); });

        double premium = calculateDynamicPremium(user, plan);
        double coverage = PLAN_CONFIG.get(plan)[1];

        Transaction credit = new Transaction();
        credit.setUser(user);
        credit.setType(Transaction.TransactionType.PAYMENT_CREDIT);
        credit.setAmount(premium);
        credit.setDescription("Payment via " + paymentMethod + " for " + plan + " plan");
        transactionRepository.save(credit);

        Policy policy = new Policy();
        policy.setUser(user);
        policy.setPlan(plan);
        policy.setStartDate(LocalDate.now());
        policy.setExpiryDate(LocalDate.now().plusDays(30));
        policy.setWeeklyPremium(premium);
        policy.setCoverageAmount(coverage);
        policyRepository.save(policy);

        Transaction debit = new Transaction();
        debit.setUser(user);
        debit.setType(Transaction.TransactionType.PREMIUM_DEBIT);
        debit.setAmount(premium);
        debit.setDescription("Weekly premium for " + plan + " plan");
        transactionRepository.save(debit);

        return policy;
    }

    public Policy getActivePolicy(User user) {
        return policyRepository.findByUserAndStatus(user, Policy.PolicyStatus.ACTIVE).orElse(null);
    }
}
