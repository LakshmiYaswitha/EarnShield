package com.gigshield.controller;

import com.gigshield.model.User;
import com.gigshield.repository.TransactionRepository;
import com.gigshield.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RiskTransactionController {

    private final RiskService riskService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/risk")
    public ResponseEntity<?> getRisk(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(riskService.getLatestRisk(user.getCity()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(transactionRepository.findByUserOrderByCreatedAtDesc(user));
    }
}
