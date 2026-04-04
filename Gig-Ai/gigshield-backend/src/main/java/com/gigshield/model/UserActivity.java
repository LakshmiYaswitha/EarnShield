package com.gigshield.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activity")
@Data
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double latitude;
    private Double longitude;
    private Double speedKmh;           // movement speed
    private Double distanceFromLast;   // km from last ping
    private String networkLocation;    // city from network/IP
    private String gpsLocation;        // city from GPS
    private boolean locationMismatch;  // network vs GPS mismatch
    private LocalDateTime recordedAt = LocalDateTime.now();
}
