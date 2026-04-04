package com.gigshield.controller;

import com.gigshield.model.Policy;
import com.gigshield.model.User;
import com.gigshield.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping("/plans")
    public ResponseEntity<?> getPlans(@AuthenticationPrincipal User user) {
        boolean highRisk = user.getRiskLevel() == User.RiskLevel.HIGH;
        return ResponseEntity.ok(List.of(
            Map.of("name", "BASIC",    "weeklyPremium", 49,  "coverage", 500,  "available", !highRisk,
                   "features", List.of("Rain protection", "Basic support")),
            Map.of("name", "STANDARD", "weeklyPremium", 99,  "coverage", 1500, "available", !highRisk,
                   "features", List.of("Rain + Heat protection", "AQI alerts", "Priority support")),
            Map.of("name", "PREMIUM",  "weeklyPremium", 199, "coverage", 3000, "available", true,
                   "features", List.of("Full protection", "Instant payouts", "24/7 support", "Fraud shield"))
        ));
    }

    @PostMapping("/activate")
    public ResponseEntity<?> activatePolicy(@AuthenticationPrincipal User user,
                                            @RequestBody Map<String, String> body) {
        Policy policy = policyService.activatePolicy(user, body.get("plan"));
        return ResponseEntity.ok(Map.of(
                "message", "Policy activated",
                "plan", policy.getPlan().name(),
                "status", policy.getStatus().name(),
                "startDate", policy.getStartDate().toString(),
                "expiryDate", policy.getExpiryDate().toString(),
                "weeklyPremium", policy.getWeeklyPremium(),
                "coverageAmount", policy.getCoverageAmount()
        ));
    }

    @PostMapping("/pay-and-activate")
    public ResponseEntity<?> payAndActivate(@AuthenticationPrincipal User user,
                                            @RequestBody Map<String, String> body) {
        Policy policy = policyService.payAndActivate(user, body.get("plan"), body.getOrDefault("paymentMethod", "Card"));
        return ResponseEntity.ok(Map.of(
                "message", "Payment successful. Policy activated!",
                "plan", policy.getPlan().name(),
                "weeklyPremium", policy.getWeeklyPremium(),
                "coverageAmount", policy.getCoverageAmount(),
                "expiryDate", policy.getExpiryDate().toString()
        ));
    }

    @GetMapping
    public ResponseEntity<?> getPolicy(@AuthenticationPrincipal User user) {
        Policy policy = policyService.getActivePolicy(user);
        if (policy == null)
            return ResponseEntity.ok(Map.of("message", "No active policy"));
        return ResponseEntity.ok(Map.of(
                "id", policy.getId(),
                "plan", policy.getPlan().name(),
                "status", policy.getStatus().name(),
                "startDate", policy.getStartDate().toString(),
                "expiryDate", policy.getExpiryDate().toString(),
                "weeklyPremium", policy.getWeeklyPremium(),
                "coverageAmount", policy.getCoverageAmount()
        ));
    }
}
