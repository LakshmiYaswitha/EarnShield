package com.gigshield.controller;

import com.gigshield.model.User;
import com.gigshield.model.Transaction;
import com.gigshield.repository.TransactionRepository;
import com.gigshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "phone", user.getPhone(),
                "city", user.getCity() != null ? user.getCity() : "",
                "persona", user.getPersona() != null ? user.getPersona().name() : "FOOD_DELIVERY",
                "weeklyEarnings", user.getWeeklyEarnings() != null ? user.getWeeklyEarnings() : 5000.0,
                "riskLevel", user.getRiskLevel() != null ? user.getRiskLevel() : User.RiskLevel.LOW,
                "walletBalance", user.getWalletBalance() != null ? user.getWalletBalance() : 0.0,
                "role", user.getRole()
        ));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal User user,
                                           @RequestBody Map<String, String> updates) {
        if (updates.containsKey("name")) user.setName(updates.get("name"));
        if (updates.containsKey("city")) user.setCity(updates.get("city"));
        if (updates.containsKey("phone")) user.setPhone(updates.get("phone"));
        if (updates.containsKey("persona")) {
            try { user.setPersona(User.Persona.valueOf(updates.get("persona"))); } catch (Exception ignored) {}
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }

    @PostMapping("/wallet/topup")
    public ResponseEntity<?> topUpWallet(@AuthenticationPrincipal User user,
                                         @RequestBody Map<String, Double> body) {
        double amount = body.getOrDefault("amount", 0.0);
        user.setWalletBalance(user.getWalletBalance() + amount);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("walletBalance", user.getWalletBalance()));
    }

    @PostMapping("/wallet/withdraw")
    public ResponseEntity<?> withdrawWallet(@AuthenticationPrincipal User user,
                                            @RequestBody Map<String, Double> body) {
        double amount = body.getOrDefault("amount", 0.0);
        if (amount <= 0) throw new RuntimeException("Enter a valid amount");
        if (amount > user.getWalletBalance()) throw new RuntimeException("Insufficient wallet balance");

        user.setWalletBalance(user.getWalletBalance() - amount);
        userRepository.save(user);

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setType(Transaction.TransactionType.REFUND);
        tx.setAmount(amount);
        tx.setDescription("Wallet withdrawal to UPI");
        transactionRepository.save(tx);

        return ResponseEntity.ok(Map.of(
                "message", "Withdrawal successful",
                "withdrawn", amount,
                "walletBalance", user.getWalletBalance()
        ));
    }
}
