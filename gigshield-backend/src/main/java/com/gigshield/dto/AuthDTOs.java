package com.gigshield.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

public class AuthDTOs {

    @Data
    public static class SignupRequest {
        @NotBlank private String name;
        @NotBlank @Email private String email;
        @NotBlank @Pattern(regexp = "^[0-9]{10}$") private String phone;
        @NotBlank @Size(min = 6) private String password;
        @NotBlank private String city;
        private String persona;
        private Double weeklyEarnings;
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private Long id;
        private String name;
        private String email;
        private String role;
        private String city;
        private String persona;
        private Double weeklyEarnings;
        private Double walletBalance;
        private String riskLevel;

        public AuthResponse(String token, com.gigshield.model.User user) {
            this.token = token;
            this.id = user.getId();
            this.name = user.getName();
            this.email = user.getEmail();
            this.role = user.getRole().name();
            this.city = user.getCity();
            this.persona = user.getPersona() != null ? user.getPersona().name() : "FOOD_DELIVERY";
            this.weeklyEarnings = user.getWeeklyEarnings() != null ? user.getWeeklyEarnings() : 5000.0;
            this.walletBalance = user.getWalletBalance();
            this.riskLevel = user.getRiskLevel() != null ? user.getRiskLevel().name() : "LOW";
        }
    }
}
