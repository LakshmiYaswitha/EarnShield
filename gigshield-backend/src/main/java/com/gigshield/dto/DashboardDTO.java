package com.gigshield.dto;

import com.gigshield.model.User;
import lombok.Data;

@Data
public class DashboardDTO {
    private String name;
    private String city;
    private User.RiskLevel riskLevel;
    private Double walletBalance;
    private String activePlan;
    private String policyStatus;
    private Double weeklyPremium;
    private Double coverageAmount;
    private int totalClaims;
    private int approvedClaims;
    private Double totalPayouts;
    private String riskSuggestion;
    private RiskSummary currentRisk;

    @Data
    public static class RiskSummary {
        private Double rainfall;
        private Double temperature;
        private Double aqi;
        private User.RiskLevel level;
    }
}
