package com.gigshield.service;

import com.gigshield.model.*;
import com.gigshield.repository.*;
import com.gigshield.service.FraudDefenseService.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClaimService {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final RiskDataRepository riskDataRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final MLService mlService;
    private final FraudDefenseService fraudDefenseService;

    public Claim triggerClaim(User user, Claim.ClaimTrigger trigger, String reason) {
        // 1. Check active policy
        Policy policy = policyRepository.findByUserAndStatus(user, Policy.PolicyStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active policy found"));

        // 2. Rule-based duplicate check
        boolean duplicate = claimRepository.existsByUserAndClaimDateAfterAndTrigger(
                user, LocalDateTime.now().minusHours(24), trigger);
        if (duplicate) throw new RuntimeException("Duplicate claim detected for today");

        // 3. Get latest risk data
        RiskData rd = riskDataRepository.findTopByCityOrderByRecordedAtDesc(user.getCity()).orElse(null);
        double rainfall = rd != null ? rd.getRainfall() : 0;
        double aqi      = rd != null ? rd.getAqi() : 50;

        // 4. ML payout prediction
        double baseAmount = policy.getCoverageAmount() * 0.3;
        Map<?, ?> payoutResult = mlService.predictPayout(
                user.getRiskLevel().name(), rainfall,
                rd != null ? rd.getTemperature() : 25,
                aqi, policy.getCoverageAmount(), policy.getPlan().name());
        double claimAmount = baseAmount;
        if (payoutResult != null && payoutResult.get("recommendedPayout") != null) {
            claimAmount = ((Number) payoutResult.get("recommendedPayout")).doubleValue();
            log.info("ML payout recommendation: {}", claimAmount);
        }

        // 5. Adversarial Defense — full behavior-based validation
        ValidationResult validation = fraudDefenseService.validateClaim(user, claimAmount, rd);

        // 6. Build claim object
        Claim claim = new Claim();
        claim.setUser(user);
        claim.setTrigger(trigger);
        claim.setTriggerReason(reason);
        claim.setAmount(claimAmount);

        // 7. Reject if high-confidence fraud
        if (validation.isFraud()) {
            claim.setFraudFlag(true);
            claim.setStatus(Claim.ClaimStatus.REJECTED);
            claimRepository.save(claim);
            messagingTemplate.convertAndSend("/topic/notifications/" + user.getId(),
                    Map.of("type", "FRAUD_DETECTED",
                            "message", "Claim rejected by AI Defense: " + String.join(", ", validation.flags())));
            throw new RuntimeException("Claim rejected: " + validation.flags().get(0));
        }

        // 8. Flag for verification if suspicious but not confirmed fraud
        if (validation.needsVerification()) {
            claim.setFraudFlag(true); // flag for admin review
            log.warn("Claim needs verification for user {}: {}", user.getId(), validation.flags());
        }

        // 9. Approve and payout
        claim.setStatus(Claim.ClaimStatus.APPROVED);
        claimRepository.save(claim);
        processPayout(user, claim);

        messagingTemplate.convertAndSend("/topic/notifications/" + user.getId(),
                Map.of("type", "CLAIM_APPROVED",
                        "message", "Claim approved: Rs." + String.format("%.2f", claim.getAmount()),
                        "trigger", trigger.name()));
        return claim;
    }

    private void processPayout(User user, Claim claim) {
        user.setWalletBalance(user.getWalletBalance() + claim.getAmount());
        userRepository.save(user);

        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setType(Transaction.TransactionType.PAYOUT_CREDIT);
        tx.setAmount(claim.getAmount());
        tx.setDescription("AI-validated payout for " + claim.getTrigger() + " claim");
        transactionRepository.save(tx);

        claim.setStatus(Claim.ClaimStatus.PAID);
        claimRepository.save(claim);
    }

    public List<Claim> getUserClaims(User user) {
        return claimRepository.findByUser(user);
    }

    public List<Claim> getAllClaims() {
        return claimRepository.findAllByOrderByClaimDateDesc();
    }
}
