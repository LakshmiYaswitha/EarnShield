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
    }

    @Data
    public static class LoginRequest {
        @NotBlank private String email;
        @NotBlank private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String name;
        private String email;
        private String role;

        public AuthResponse(String token, String name, String email, String role) {
            this.token = token;
            this.name = name;
            this.email = email;
            this.role = role;
        }
    }
}
