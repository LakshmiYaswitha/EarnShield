package com.gigshield.service;

import com.gigshield.dto.DashboardDTO;
import com.gigshield.model.*;
import com.gigshield.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final RiskService riskService;

    public DashboardDTO getDashboard(User user) {
        DashboardDTO dto = new DashboardDTO();
        dto.setName(user.getName());
        dto.setCity(user.getCity());
        dto.setRiskLevel(user.getRiskLevel());
        dto.setWalletBalance(user.getWalletBalance());

        Policy policy = policyRepository.findByUserAndStatus(user, Policy.PolicyStatus.ACTIVE).orElse(null);
        if (policy != null) {
            dto.setActivePlan(policy.getPlan().name());
            dto.setPolicyStatus(policy.getStatus().name());
            dto.setWeeklyPremium(policy.getWeeklyPremium());
            dto.setCoverageAmount(policy.getCoverageAmount());
        }

        List<Claim> claims = claimRepository.findByUser(user);
        dto.setTotalClaims(claims.size());
        dto.setApprovedClaims((int) claims.stream()
                .filter(c -> c.getStatus() == Claim.ClaimStatus.PAID).count());
        dto.setTotalPayouts(claims.stream()
                .filter(c -> c.getStatus() == Claim.ClaimStatus.PAID)
                .mapToDouble(Claim::getAmount).sum());

        RiskData rd = riskService.getLatestRisk(user.getCity());
        DashboardDTO.RiskSummary rs = new DashboardDTO.RiskSummary();
        rs.setRainfall(rd.getRainfall());
        rs.setTemperature(rd.getTemperature());
        rs.setAqi(rd.getAqi());
        rs.setLevel(rd.getRiskLevel());
        dto.setCurrentRisk(rs);
        dto.setRiskSuggestion(riskService.getRiskSuggestion(user.getRiskLevel(), rd));

        return dto;
    }
}
