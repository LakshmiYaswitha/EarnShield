package com.gigshield.service;

import com.gigshield.model.RiskData;
import com.gigshield.model.User;
import com.gigshield.repository.RiskDataRepository;
import com.gigshield.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final RiskDataRepository riskDataRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${weather.api.key}") private String weatherKey;
    @Value("${weather.api.url}") private String weatherUrl;
    @Value("${aqi.api.key}") private String aqiKey;
    @Value("${aqi.api.url}") private String aqiUrl;
    @Value("${risk.rain.threshold}") private double rainThreshold;
    @Value("${risk.temp.threshold}") private double tempThreshold;
    @Value("${risk.aqi.threshold}") private double aqiThreshold;

    public RiskData fetchAndStoreRisk(String city) {
        double rainfall = 0, temperature = 25, aqi = 50;

        try {
            String wUrl = weatherUrl + "?q=" + city + "&appid=" + weatherKey + "&units=metric";
            Map<?, ?> wResp = restTemplate.getForObject(wUrl, Map.class);
            if (wResp != null) {
                Map<?, ?> main = (Map<?, ?>) wResp.get("main");
                if (main != null) temperature = ((Number) main.get("temp")).doubleValue();
                Map<?, ?> rain = (Map<?, ?>) wResp.get("rain");
                if (rain != null && rain.get("1h") != null)
                    rainfall = ((Number) rain.get("1h")).doubleValue();
            }
        } catch (Exception ignored) {}

        try {
            String aUrl = aqiUrl + "/" + city + "/?token=" + aqiKey;
            Map<?, ?> aResp = restTemplate.getForObject(aUrl, Map.class);
            if (aResp != null) {
                Map<?, ?> data = (Map<?, ?>) aResp.get("data");
                if (data != null && data.get("aqi") != null)
                    aqi = ((Number) data.get("aqi")).doubleValue();
            }
        } catch (Exception ignored) {}

        User.RiskLevel level = calculateRiskLevel(rainfall, temperature, aqi);

        RiskData rd = new RiskData();
        rd.setCity(city);
        rd.setRainfall(rainfall);
        rd.setTemperature(temperature);
        rd.setAqi(aqi);
        rd.setRiskLevel(level);
        riskDataRepository.save(rd);

        // Update all users in this city
        userRepository.findAll().stream()
                .filter(u -> city.equalsIgnoreCase(u.getCity()))
                .forEach(u -> { u.setRiskLevel(level); userRepository.save(u); });

        return rd;
    }

    public User.RiskLevel calculateRiskLevel(double rain, double temp, double aqi) {
        int score = 0;
        if (rain > rainThreshold) score += 2;
        if (temp > tempThreshold) score += 2;
        if (aqi > aqiThreshold) score += 2;
        if (rain > rainThreshold / 2) score += 1;
        if (temp > tempThreshold - 5) score += 1;
        if (aqi > aqiThreshold - 50) score += 1;

        if (score >= 4) return User.RiskLevel.HIGH;
        if (score >= 2) return User.RiskLevel.MEDIUM;
        return User.RiskLevel.LOW;
    }

    public RiskData getLatestRisk(String city) {
        return riskDataRepository.findTopByCityOrderByRecordedAtDesc(city)
                .orElseGet(() -> fetchAndStoreRisk(city));
    }

    public String getRiskSuggestion(User.RiskLevel level, RiskData rd) {
        if (level == User.RiskLevel.HIGH) {
            if (rd.getRainfall() > rainThreshold) return "Heavy rain expected. Consider working early morning to avoid disruption.";
            if (rd.getTemperature() > tempThreshold) return "Extreme heat alert. Stay hydrated and limit midday deliveries.";
            if (rd.getAqi() > aqiThreshold) return "Poor air quality. Wear a mask and reduce outdoor exposure.";
        }
        if (level == User.RiskLevel.MEDIUM) return "Moderate risk today. Monitor conditions and plan your shifts accordingly.";
        return "Conditions look good. Safe to work normally today.";
    }
}
