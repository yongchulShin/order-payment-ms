package com.example.authservice.service;

import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.TokenInfo;
import com.example.authservice.dto.TokenResponse;
import com.example.authservice.model.Auth;
import com.example.authservice.model.AuthStatus;
import com.example.authservice.model.User;
import com.example.authservice.repository.AuthRepository;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.util.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    @Transactional
    public TokenResponse login(LoginRequest loginRequest) {
        log.info("Attempting login for user: {}", loginRequest.getEmail());
        
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            TokenInfo tokenInfo = (TokenInfo) authentication.getPrincipal();
            log.debug("User authenticated successfully: {}", tokenInfo.getUsername());
            
            // Get user
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + loginRequest.getEmail()));
            
            // Invalidate existing active tokens
            authRepository.findByUserAndStatus(user, AuthStatus.ACTIVE)
                    .ifPresent(existingAuth -> {
                        existingAuth.setStatus(AuthStatus.LOGGED_OUT);
                        authRepository.save(existingAuth);
                        log.debug("Invalidated existing active token for user: {}", user.getEmail());
                    });

            // Create tokens
            String accessToken = tokenProvider.createAccessToken(authentication);
            String refreshToken = tokenProvider.createRefreshToken();

            // Save auth information
            Auth auth = Auth.builder()
                    .user(user)
                    .refreshToken(refreshToken)
                    .tokenExpiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenValidityInMilliseconds() / 1000))
                    .status(AuthStatus.ACTIVE)
                    .build();
            
            auth.updateLastLogin();
            authRepository.save(auth);
            
            log.info("Login successful for user: {}", loginRequest.getEmail());

            return TokenResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(300L)
                    .build();
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getEmail(), e);
            throw e;
        }
    }

    @Transactional
    public TokenResponse refreshToken(String oldRefreshToken) {
        log.info("Attempting to refresh token");
        
        try {
            // Find and validate existing auth
            Auth auth = authRepository.findByRefreshTokenAndStatus(oldRefreshToken, AuthStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

            if (auth.isTokenExpired()) {
                log.warn("Refresh token expired for user: {}", auth.getUser().getEmail());
                auth.setStatus(AuthStatus.EXPIRED);
                authRepository.save(auth);
                throw new IllegalArgumentException("Refresh token expired");
            }

            // Create new tokens
            Authentication authentication = tokenProvider.getAuthentication(oldRefreshToken);
            String newAccessToken = tokenProvider.createAccessToken(authentication);
            String newRefreshToken = tokenProvider.createRefreshToken();

            // Update auth status and create new auth
            auth.setStatus(AuthStatus.LOGGED_OUT);
            authRepository.save(auth);

            Auth newAuth = Auth.builder()
                    .user(auth.getUser())
                    .refreshToken(newRefreshToken)
                    .tokenExpiryDate(LocalDateTime.now().plusSeconds(tokenProvider.getRefreshTokenValidityInMilliseconds() / 1000))
                    .status(AuthStatus.ACTIVE)
                    .build();
            
            newAuth.updateLastAccess();
            authRepository.save(newAuth);
            
            log.info("Token refresh successful for user: {}", auth.getUser().getEmail());

            return TokenResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(300L)
                    .build();
        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw e;
        }
    }

    @Transactional
    public void logout(String refreshToken) {
        log.info("Attempting logout");
        
        try {
            Auth auth = authRepository.findByRefreshTokenAndStatus(refreshToken, AuthStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
            
            auth.setStatus(AuthStatus.LOGGED_OUT);
            authRepository.save(auth);
            
            log.info("Logout successful for user: {}", auth.getUser().getEmail());
        } catch (Exception e) {
            log.error("Logout failed", e);
            throw e;
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // Run at midnight every day
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting expired tokens cleanup");
        
        try {
            List<Auth> expiredAuths = authRepository.findExpiredTokens(LocalDateTime.now(), AuthStatus.ACTIVE);
            expiredAuths.forEach(auth -> auth.setStatus(AuthStatus.EXPIRED));
            authRepository.saveAll(expiredAuths);
            
            log.info("Expired tokens cleanup completed. {} tokens processed", expiredAuths.size());
        } catch (Exception e) {
            log.error("Error during expired tokens cleanup", e);
            throw e;
        }
    }
} 