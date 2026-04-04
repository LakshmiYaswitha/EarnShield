package com.gigshield.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MLService {

    private final RestTemplate restTemplate;
    private static final String ML_BASE = "http://localhost:5000";

    public boolean isMLAvailable() {
        try {
            restTemplate.getForObject(ML_BASE + "/health", Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Risk prediction from ML model
    public Map<?, ?> predictRisk(double rainfall, double temperature, double aqi) {
        try {
            Map<String, Double> body = Map.of(
                    "rainfall", rainfall,
                    "temperature", temperature,
                    "aqi", aqi
            );
            return restTemplate.postForObject(ML_BASE + "/predict/risk", body, Map.class);
        } catch (Exception e) {
            log.warn("ML risk prediction failed, using rule-based fallback: {}", e.getMessage());
            return null;
        }
    }

    // Fraud detection from ML model
    public Map<?, ?> detectFraud(int claimsLast7Days, double claimAmount,
                                  int daysSincePolicyStart, double aqi, double rainfall) {
        try {
            Map<String, Object> body = Map.of(
                    "claimsLast7Days", claimsLast7Days,
                    "claimAmount", claimAmount,
                    "daysSincePolicyStart", daysSincePolicyStart,
                    "aqi", aqi,
                    "rainfall", rainfall
            );
            return restTemplate.postForObject(ML_BASE + "/predict/fraud", body, Map.class);
        } catch (Exception e) {
            log.warn("ML fraud detection failed: {}", e.getMessage());
            return null;
        }
    }

    // Payout calculation from ML model
    public Map<?, ?> predictPayout(String riskLevel, double rainfall, double temperature,
                                    double aqi, double coverageAmount, String planType) {
        try {
            Map<String, Object> body = Map.of(
                    "riskLevel", riskLevel,
                    "rainfall", rainfall,
                    "temperature", temperature,
                    "aqi", aqi,
                    "coverageAmount", coverageAmount,
                    "planType", planType
            );
            return restTemplate.postForObject(ML_BASE + "/predict/payout", body, Map.class);
        } catch (Exception e) {
            log.warn("ML payout prediction failed: {}", e.getMessage());
            return null;
        }
    }

    // Full analysis — all 3 models in one call
    public Map<?, ?> fullAnalysis(double rainfall, double temperature, double aqi,
                                   int claimsLast7Days, double claimAmount,
                                   int daysSincePolicyStart, double coverageAmount, String planType) {
        try {
            Map<String, Object> body = Map.of(
                    "rainfall", rainfall,
                    "temperature", temperature,
                    "aqi", aqi,
                    "claimsLast7Days", claimsLast7Days,
                    "claimAmount", claimAmount,
                    "daysSincePolicyStart", daysSincePolicyStart,
                    "coverageAmount", coverageAmount,
                    "planType", planType
            );
            return restTemplate.postForObject(ML_BASE + "/predict/analyze", body, Map.class);
        } catch (Exception e) {
            log.warn("ML full analysis failed: {}", e.getMessage());
            return null;
        }
    }
}
