package com.gigshield.controller;

import com.gigshield.model.FraudAlert;
import com.gigshield.model.User;
import com.gigshield.service.FraudDefenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
public class FraudDefenseController {

    private final FraudDefenseService fraudDefenseService;

    // User records their activity (location ping)
    @PostMapping("/activity")
    public ResponseEntity<?> recordActivity(@AuthenticationPrincipal User user,
                                             @RequestBody Map<String, Object> body) {
        Double lat     = body.get("latitude")  != null ? ((Number) body.get("latitude")).doubleValue()  : null;
        Double lon     = body.get("longitude") != null ? ((Number) body.get("longitude")).doubleValue() : null;
        Double speed   = body.get("speed")     != null ? ((Number) body.get("speed")).doubleValue()     : null;
        String netCity = (String) body.getOrDefault("networkCity", user.getCity());
        String gpsCity = (String) body.getOrDefault("gpsCity",     user.getCity());

        fraudDefenseService.recordActivity(user, lat, lon, speed, netCity, gpsCity);
        return ResponseEntity.ok(Map.of("message", "Activity recorded"));
    }

    // User sees their own fraud alerts
    @GetMapping("/alerts/me")
    public ResponseEntity<?> myAlerts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toMapList(fraudDefenseService.getUserAlerts(user)));
    }

    // Admin — all unresolved alerts
    @GetMapping("/alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> allAlerts() {
        return ResponseEntity.ok(toMapList(fraudDefenseService.getAllAlerts()));
    }

    // Admin — resolve an alert
    @PutMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolveAlert(@PathVariable Long id) {
        fraudDefenseService.resolveAlert(id);
        return ResponseEntity.ok(Map.of("message", "Alert resolved"));
    }

    // Admin — fraud summary stats
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fraudStats() {
        List<FraudAlert> all = fraudDefenseService.getAllAlerts();
        long unresolved = all.stream().filter(a -> !a.isResolved()).count();
        long critical   = all.stream().filter(a -> a.getSeverity() == FraudAlert.AlertSeverity.CRITICAL).count();
        long gpsSpoofing = all.stream().filter(a -> a.getAlertType() == FraudAlert.AlertType.GPS_SPOOFING).count();
        long groupFraud  = all.stream().filter(a -> a.getAlertType() == FraudAlert.AlertType.GROUP_FRAUD).count();

        return ResponseEntity.ok(Map.of(
                "totalAlerts",    all.size(),
                "unresolved",     unresolved,
                "critical",       critical,
                "gpsSpoofing",    gpsSpoofing,
                "groupFraud",     groupFraud
        ));
    }

    private List<Map<String, Object>> toMapList(List<FraudAlert> alerts) {
        return alerts.stream().map(a -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("id",          a.getId());
            m.put("alertType",   a.getAlertType().name());
            m.put("severity",    a.getSeverity().name());
            m.put("description", a.getDescription());
            m.put("resolved",    a.isResolved());
            m.put("createdAt",   a.getCreatedAt().toString());
            m.put("userId",      a.getUser().getId());
            m.put("userName",    a.getUser().getName());
            return m;
        }).collect(Collectors.toList());
    }
}
