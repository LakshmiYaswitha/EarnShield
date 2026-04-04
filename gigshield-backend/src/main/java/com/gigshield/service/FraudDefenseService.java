package com.gigshield.service;

import com.gigshield.model.*;
import com.gigshield.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDefenseService {

    private final UserActivityRepository activityRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final ClaimRepository claimRepository;

    // Max realistic delivery speed in km/h
    private static final double MAX_DELIVERY_SPEED = 80.0;
    // Max realistic location jump in km within 10 minutes
    private static final double MAX_LOCATION_JUMP_KM = 15.0;
    // Group fraud threshold — claims from same city in 1 hour
    private static final long GROUP_FRAUD_THRESHOLD = 5;

    // ─────────────────────────────────────────────
    // MAIN VALIDATION — called before every claim
    // Returns: ValidationResult with verdict + reasons
    // ─────────────────────────────────────────────
    public ValidationResult validateClaim(User user, double claimAmount, RiskData riskData) {
        List<String> flags = new ArrayList<>();
        FraudAlert.AlertSeverity maxSeverity = FraudAlert.AlertSeverity.LOW;

        // 1. Check movement patterns
        MovementCheck movement = checkMovementPattern(user);
        if (movement.suspicious()) {
            flags.add(movement.reason());
            createAlert(user, movement.alertType(), movement.severity(), movement.reason());
            if (movement.severity().ordinal() > maxSeverity.ordinal())
                maxSeverity = movement.severity();
        }

        // 2. Check network vs GPS location mismatch
        LocationCheck location = checkLocationConsistency(user);
        if (location.mismatch()) {
            flags.add(location.reason());
            createAlert(user, FraudAlert.AlertType.NETWORK_GPS_MISMATCH,
                    FraudAlert.AlertSeverity.HIGH, location.reason());
            maxSeverity = FraudAlert.AlertSeverity.HIGH;
        }

        // 3. Check duplicate claims
        DuplicateCheck duplicate = checkDuplicateClaims(user);
        if (duplicate.duplicate()) {
            flags.add(duplicate.reason());
            createAlert(user, FraudAlert.AlertType.DUPLICATE_CLAIM,
                    FraudAlert.AlertSeverity.MEDIUM, duplicate.reason());
            if (FraudAlert.AlertSeverity.MEDIUM.ordinal() > maxSeverity.ordinal())
                maxSeverity = FraudAlert.AlertSeverity.MEDIUM;
        }

        // 4. Check suspicious timing
        TimingCheck timing = checkClaimTiming(user);
        if (timing.suspicious()) {
            flags.add(timing.reason());
            createAlert(user, FraudAlert.AlertType.SUSPICIOUS_TIMING,
                    FraudAlert.AlertSeverity.MEDIUM, timing.reason());
        }

        // 5. Check group fraud
        GroupFraudCheck group = checkGroupFraud(user.getCity());
        if (group.groupFraud()) {
            flags.add(group.reason());
            createAlert(user, FraudAlert.AlertType.GROUP_FRAUD,
                    FraudAlert.AlertSeverity.CRITICAL, group.reason());
            maxSeverity = FraudAlert.AlertSeverity.CRITICAL;
        }

        // 6. Validate weather conditions
        WeatherCheck weather = checkWeatherConsistency(user, riskData);
        if (!weather.consistent()) {
            flags.add(weather.reason());
            createAlert(user, FraudAlert.AlertType.BEHAVIOR_ANOMALY,
                    FraudAlert.AlertSeverity.HIGH, weather.reason());
        }

        boolean isFraud = !flags.isEmpty() && maxSeverity.ordinal() >= FraudAlert.AlertSeverity.HIGH.ordinal();
        boolean needsVerification = !flags.isEmpty() && !isFraud;

        return new ValidationResult(isFraud, needsVerification, flags, maxSeverity);
    }

    // ─────────────────────────────────────────────
    // 1. MOVEMENT PATTERN CHECK
    // ─────────────────────────────────────────────
    private MovementCheck checkMovementPattern(User user) {
        List<UserActivity> recent = activityRepository
                .findByUserAndRecordedAtAfterOrderByRecordedAtDesc(user, LocalDateTime.now().minusHours(1));

        if (recent.size() < 2) return new MovementCheck(false, null, null, null);

        UserActivity latest = recent.get(0);
        UserActivity previous = recent.get(1);

        if (latest.getSpeedKmh() != null && latest.getSpeedKmh() > MAX_DELIVERY_SPEED) {
            return new MovementCheck(true,
                    "Abnormal speed detected: " + String.format("%.1f", latest.getSpeedKmh()) + " km/h (max allowed: " + MAX_DELIVERY_SPEED + " km/h)",
                    FraudAlert.AlertType.ABNORMAL_SPEED, FraudAlert.AlertSeverity.HIGH);
        }

        if (latest.getLatitude() != null && previous.getLatitude() != null) {
            double dist = haversineDistance(
                    previous.getLatitude(), previous.getLongitude(),
                    latest.getLatitude(), latest.getLongitude());
            if (dist > MAX_LOCATION_JUMP_KM) {
                return new MovementCheck(true,
                        "Impossible location jump: " + String.format("%.1f", dist) + " km in under 10 minutes",
                        FraudAlert.AlertType.LOCATION_JUMP, FraudAlert.AlertSeverity.CRITICAL);
            }
        }

        return new MovementCheck(false, null, null, null);
    }

    // ─────────────────────────────────────────────
    // 2. NETWORK vs GPS LOCATION CHECK
    // ─────────────────────────────────────────────
    private LocationCheck checkLocationConsistency(User user) {
        Optional<UserActivity> latest = activityRepository.findTopByUserOrderByRecordedAtDesc(user);
        if (latest.isEmpty()) return new LocationCheck(false, null);
        UserActivity act = latest.get();
        if (act.isLocationMismatch()) {
            return new LocationCheck(true,
                    "Network location (" + act.getNetworkLocation() + ") does not match GPS location (" + act.getGpsLocation() + ") - possible GPS spoofing");
        }
        return new LocationCheck(false, null);
    }

    // ─────────────────────────────────────────────
    // 3. DUPLICATE CLAIM CHECK
    // ─────────────────────────────────────────────
    private DuplicateCheck checkDuplicateClaims(User user) {
        long claimsToday = claimRepository.findByUserAndClaimDateAfter(user, LocalDateTime.now().minusHours(24)).size();
        if (claimsToday >= 3) {
            return new DuplicateCheck(true,
                    "Excessive claims: " + claimsToday + " claims filed in last 24 hours");
        }
        return new DuplicateCheck(false, null);
    }

    // ─────────────────────────────────────────────
    // 4. SUSPICIOUS TIMING CHECK
    // ─────────────────────────────────────────────
    private TimingCheck checkClaimTiming(User user) {
        long alertsThisWeek = fraudAlertRepository.countByUserAndCreatedAtAfter(user, LocalDateTime.now().minusDays(7));
        if (alertsThisWeek >= 2) {
            return new TimingCheck(true,
                    "User has " + alertsThisWeek + " fraud alerts in the past 7 days — high-risk profile");
        }
        return new TimingCheck(false, null);
    }

    // ─────────────────────────────────────────────
    // 5. GROUP FRAUD CHECK
    // ─────────────────────────────────────────────
    private GroupFraudCheck checkGroupFraud(String city) {
        List<Claim> recentCityClaimsRaw = claimRepository.findAllByOrderByClaimDateDesc();
        long cityClaimsLastHour = recentCityClaimsRaw.stream()
                .filter(c -> c.getUser().getCity().equalsIgnoreCase(city))
                .filter(c -> c.getClaimDate().isAfter(LocalDateTime.now().minusHours(1)))
                .count();

        if (cityClaimsLastHour >= GROUP_FRAUD_THRESHOLD) {
            return new GroupFraudCheck(true,
                    "Group fraud pattern: " + cityClaimsLastHour + " claims from " + city + " in last hour — stricter verification applied");
        }
        return new GroupFraudCheck(false, null);
    }

    // ─────────────────────────────────────────────
    // 6. WEATHER CONSISTENCY CHECK
    // ─────────────────────────────────────────────
    private WeatherCheck checkWeatherConsistency(User user, RiskData riskData) {
        if (riskData == null) return new WeatherCheck(true, null);

        // If user claims rain but actual rainfall is 0 — suspicious
        if (riskData.getRainfall() != null && riskData.getRainfall() < 1.0
                && riskData.getTemperature() != null && riskData.getTemperature() < 35.0
                && riskData.getAqi() != null && riskData.getAqi() < 100.0) {
            return new WeatherCheck(false,
                    "Weather data does not support claim: Rain=" + riskData.getRainfall()
                            + "mm, Temp=" + riskData.getTemperature() + "C, AQI=" + riskData.getAqi());
        }
        return new WeatherCheck(true, null);
    }

    // ─────────────────────────────────────────────
    // RECORD USER ACTIVITY (called from controller)
    // ─────────────────────────────────────────────
    public UserActivity recordActivity(User user, Double lat, Double lon,
                                        Double speed, String networkCity, String gpsCity) {
        UserActivity activity = new UserActivity();
        activity.setUser(user);
        activity.setLatitude(lat);
        activity.setLongitude(lon);
        activity.setSpeedKmh(speed);
        activity.setNetworkLocation(networkCity);
        activity.setGpsLocation(gpsCity);
        activity.setLocationMismatch(networkCity != null && gpsCity != null
                && !networkCity.equalsIgnoreCase(gpsCity));

        // Calculate distance from last ping
        activityRepository.findTopByUserOrderByRecordedAtDesc(user).ifPresent(prev -> {
            if (prev.getLatitude() != null && lat != null) {
                double dist = haversineDistance(prev.getLatitude(), prev.getLongitude(), lat, lon);
                activity.setDistanceFromLast(dist);
            }
        });

        return activityRepository.save(activity);
    }

    private void createAlert(User user, FraudAlert.AlertType type,
                              FraudAlert.AlertSeverity severity, String description) {
        FraudAlert alert = new FraudAlert();
        alert.setUser(user);
        alert.setAlertType(type);
        alert.setSeverity(severity);
        alert.setDescription(description);
        fraudAlertRepository.save(alert);
        log.warn("FRAUD ALERT [{}] for user {}: {}", severity, user.getId(), description);
    }

    // Haversine formula — distance between two GPS coordinates in km
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public List<FraudAlert> getUnresolvedAlerts() {
        return fraudAlertRepository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    public List<FraudAlert> getAllAlerts() {
        return fraudAlertRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<FraudAlert> getUserAlerts(User user) {
        return fraudAlertRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public void resolveAlert(Long alertId) {
        fraudAlertRepository.findById(alertId).ifPresent(a -> {
            a.setResolved(true);
            fraudAlertRepository.save(a);
        });
    }

    // ─────────────────────────────────────────────
    // INNER RESULT CLASSES
    // ─────────────────────────────────────────────
    public record ValidationResult(boolean isFraud, boolean needsVerification,
                                    List<String> flags, FraudAlert.AlertSeverity severity) {}

    private record MovementCheck(boolean suspicious, String reason,
                                  FraudAlert.AlertType alertType, FraudAlert.AlertSeverity severity) {}

    private record LocationCheck(boolean mismatch, String reason) {}
    private record DuplicateCheck(boolean duplicate, String reason) {}
    private record TimingCheck(boolean suspicious, String reason) {}
    private record GroupFraudCheck(boolean groupFraud, String reason) {}
    private record WeatherCheck(boolean consistent, String reason) {}
}
