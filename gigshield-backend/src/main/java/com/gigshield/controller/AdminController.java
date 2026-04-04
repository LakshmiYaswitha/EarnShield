package com.gigshield.controller;

import com.gigshield.model.User;
import com.gigshield.repository.UserRepository;
import com.gigshield.service.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final ClaimService claimService;

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(userRepository.findAll().stream().map(u -> Map.of(
                "id", u.getId(), "name", u.getName(), "email", u.getEmail(),
                "city", u.getCity() != null ? u.getCity() : "",
                "riskLevel", u.getRiskLevel(), "walletBalance", u.getWalletBalance()
        )).collect(Collectors.toList()));
    }

    @GetMapping("/claims")
    public ResponseEntity<?> getClaims() {
        return ResponseEntity.ok(claimService.getAllClaims());
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers = userRepository.count();
        long highRisk = userRepository.findAll().stream()
                .filter(u -> u.getRiskLevel() == User.RiskLevel.HIGH).count();
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "highRiskUsers", highRisk,
                "totalClaims", claimService.getAllClaims().size()
        ));
    }
}
