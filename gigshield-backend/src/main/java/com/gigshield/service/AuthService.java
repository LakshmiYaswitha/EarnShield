package com.gigshield.service;

import com.gigshield.dto.AuthDTOs;
import com.gigshield.model.User;
import com.gigshield.repository.UserRepository;
import com.gigshield.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthDTOs.AuthResponse signup(AuthDTOs.SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("Email already registered");
        if (userRepository.existsByPhone(req.getPhone()))
            throw new RuntimeException("Phone already registered");

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setCity(req.getCity());
        if (req.getPersona() != null) {
            try { user.setPersona(User.Persona.valueOf(req.getPersona())); } catch (Exception ignored) {}
        }
        if (req.getWeeklyEarnings() != null) user.setWeeklyEarnings(req.getWeeklyEarnings());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthDTOs.AuthResponse(token, user);
    }

    public AuthDTOs.AuthResponse login(AuthDTOs.LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new RuntimeException("Invalid credentials");

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthDTOs.AuthResponse(token, user);
    }
}
