package com.gigshield.controller;

import com.gigshield.model.Claim;
import com.gigshield.model.User;
import com.gigshield.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    @GetMapping
    public ResponseEntity<?> getClaims(@AuthenticationPrincipal User user) {
        List<Map<String, Object>> claims = claimService.getUserClaims(user).stream()
                .map(this::toMap).collect(Collectors.toList());
        return ResponseEntity.ok(claims);
    }

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerManualClaim(@AuthenticationPrincipal User user,
                                                @RequestBody Map<String, String> body) {
        Claim claim = claimService.triggerClaim(user, Claim.ClaimTrigger.MANUAL,
                body.getOrDefault("reason", "Manual claim request"));
        return ResponseEntity.ok(Map.of(
                "message", "Claim triggered",
                "amount", claim.getAmount(),
                "status", claim.getStatus().name()
        ));
    }

    private Map<String, Object> toMap(Claim c) {
        return Map.of(
                "id", c.getId(),
                "trigger", c.getTrigger().name(),
                "status", c.getStatus().name(),
                "amount", c.getAmount(),
                "triggerReason", c.getTriggerReason() != null ? c.getTriggerReason() : "",
                "claimDate", c.getClaimDate().toString(),
                "fraudFlag", c.isFraudFlag()
        );
    }
}
