package com.gigshield.controller;

import com.gigshield.model.User;
import com.gigshield.service.MLService;
import com.gigshield.service.RiskService;
import com.gigshield.model.RiskData;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MLController {

    private final MLService mlService;
    private final RiskService riskService;

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        boolean available = mlService.isMLAvailable();
        return ResponseEntity.ok(Map.of(
                "mlAvailable", available,
                "models", available
                        ? new String[]{"risk_prediction", "fraud_detection", "payout_calculation"}
                        : new String[]{},
                "message", available ? "ML service running on port 5000" : "ML service offline - using rule-based fallback"
        ));
    }

    @PostMapping("/predict/risk")
    public ResponseEntity<?> predictRisk(@RequestBody Map<String, Double> body) {
        double rainfall    = body.getOrDefault("rainfall", 0.0);
        double temperature = body.getOrDefault("temperature", 25.0);
        double aqi         = body.getOrDefault("aqi", 50.0);

        Map<?, ?> mlResult = mlService.predictRisk(rainfall, temperature, aqi);
        if (mlResult != null) return ResponseEntity.ok(mlResult);

        // Fallback to rule-based
        User.RiskLevel level = riskService.calculateRiskLevel(rainfall, temperature, aqi);
        return ResponseEntity.ok(Map.of("riskLevel", level.name(), "source", "rule-based-fallback"));
    }

    @PostMapping("/predict/fraud")
    public ResponseEntity<?> detectFraud(@RequestBody Map<String, Object> body) {
        int claimsLast7Days       = ((Number) body.getOrDefault("claimsLast7Days", 0)).intValue();
        double claimAmount        = ((Number) body.getOrDefault("claimAmount", 0)).doubleValue();
        int daysSincePolicyStart  = ((Number) body.getOrDefault("daysSincePolicyStart", 1)).intValue();
        double aqi                = ((Number) body.getOrDefault("aqi", 50)).doubleValue();
        double rainfall           = ((Number) body.getOrDefault("rainfall", 0)).doubleValue();

        Map<?, ?> result = mlService.detectFraud(claimsLast7Days, claimAmount, daysSincePolicyStart, aqi, rainfall);
        if (result != null) return ResponseEntity.ok(result);
        return ResponseEntity.ok(Map.of("isFraud", false, "verdict", "CLEAN", "source", "fallback"));
    }

    @PostMapping("/predict/payout")
    public ResponseEntity<?> predictPayout(@RequestBody Map<String, Object> body) {
        String riskLevel    = (String) body.getOrDefault("riskLevel", "LOW");
        double rainfall     = ((Number) body.getOrDefault("rainfall", 0)).doubleValue();
        double temperature  = ((Number) body.getOrDefault("temperature", 25)).doubleValue();
        double aqi          = ((Number) body.getOrDefault("aqi", 50)).doubleValue();
        double coverage     = ((Number) body.getOrDefault("coverageAmount", 500)).doubleValue();
        String planType     = (String) body.getOrDefault("planType", "BASIC");

        Map<?, ?> result = mlService.predictPayout(riskLevel, rainfall, temperature, aqi, coverage, planType);
        if (result != null) return ResponseEntity.ok(result);
        return ResponseEntity.ok(Map.of("recommendedPayout", coverage * 0.3, "source", "fallback"));
    }

    @GetMapping("/analyze")
    public ResponseEntity<?> analyze(@AuthenticationPrincipal User user) {
        RiskData rd = riskService.getLatestRisk(user.getCity());
        Map<?, ?> result = mlService.fullAnalysis(
                rd.getRainfall(), rd.getTemperature(), rd.getAqi(),
                0, 0, 1, 500, "BASIC"
        );
        if (result != null) return ResponseEntity.ok(result);
        return ResponseEntity.ok(Map.of("riskLevel", user.getRiskLevel().name(), "source", "fallback"));
    }
}
