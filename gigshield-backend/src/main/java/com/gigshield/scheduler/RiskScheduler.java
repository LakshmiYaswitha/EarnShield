package com.gigshield.scheduler;

import com.gigshield.model.*;
import com.gigshield.repository.*;
import com.gigshield.service.ClaimService;
import com.gigshield.service.RiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RiskScheduler {

    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final RiskService riskService;
    private final ClaimService claimService;

    @Value("${risk.rain.threshold}") private double rainThreshold;
    @Value("${risk.temp.threshold}") private double tempThreshold;
    @Value("${risk.aqi.threshold}") private double aqiThreshold;

    @Scheduled(fixedRateString = "3600000") // every 1 hour
    public void runRiskCheck() {
        log.info("Running scheduled risk check...");

        // Get distinct cities with active policies
        List<String> cities = userRepository.findAll().stream()
                .filter(u -> policyRepository.findByUserAndStatus(u, Policy.PolicyStatus.ACTIVE).isPresent())
                .map(User::getCity)
                .distinct()
                .collect(Collectors.toList());

        for (String city : cities) {
            try {
                RiskData rd = riskService.fetchAndStoreRisk(city);
                checkAndTriggerClaims(city, rd);
            } catch (Exception e) {
                log.error("Risk check failed for city {}: {}", city, e.getMessage());
            }
        }
    }

    private void checkAndTriggerClaims(String city, RiskData rd) {
        List<User> usersInCity = userRepository.findAll().stream()
                .filter(u -> city.equalsIgnoreCase(u.getCity()))
                .collect(Collectors.toList());

        // Mock flood: rainfall > 50mm triggers flood alert
        boolean flood  = rd.getRainfall() != null && rd.getRainfall() > 50.0;
        // Mock curfew: simulate based on city name hash (demo purposes)
        boolean curfew = (city.hashCode() % 7 == 0);
        rd.setFloodAlert(flood);
        rd.setCurfewAlert(curfew);

        for (User user : usersInCity) {
            if (policyRepository.findByUserAndStatus(user, Policy.PolicyStatus.ACTIVE).isEmpty()) continue;

            try {
                if (flood) {
                    claimService.triggerClaim(user, Claim.ClaimTrigger.FLOOD,
                            "Flood alert: Rainfall " + rd.getRainfall() + "mm — severe flooding detected");
                } else if (curfew) {
                    claimService.triggerClaim(user, Claim.ClaimTrigger.CURFEW,
                            "Curfew/Strike alert in " + city + " — work disruption detected");
                } else if (rd.getRainfall() > rainThreshold) {
                    claimService.triggerClaim(user, Claim.ClaimTrigger.RAIN,
                            "Rainfall " + rd.getRainfall() + "mm exceeds threshold");
                } else if (rd.getTemperature() > tempThreshold) {
                    claimService.triggerClaim(user, Claim.ClaimTrigger.HEAT,
                            "Temperature " + rd.getTemperature() + "°C exceeds threshold");
                } else if (rd.getAqi() > aqiThreshold) {
                    claimService.triggerClaim(user, Claim.ClaimTrigger.AQI,
                            "AQI " + rd.getAqi() + " exceeds safe threshold");
                }
            } catch (Exception e) {
                log.debug("Claim skip for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
