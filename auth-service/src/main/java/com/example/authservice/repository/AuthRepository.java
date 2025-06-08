package com.example.authservice.repository;

import com.example.authservice.model.Auth;
import com.example.authservice.model.AuthStatus;
import com.example.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByRefreshTokenAndStatus(String refreshToken, AuthStatus status);
    
    Optional<Auth> findByUserAndStatus(User user, AuthStatus status);
    
    List<Auth> findAllByUser(User user);
    
    @Query("SELECT a FROM Auth a WHERE a.user = :user AND a.status = :status AND a.tokenExpiryDate > :now")
    Optional<Auth> findValidAuthByUser(@Param("user") User user, 
                                     @Param("status") AuthStatus status,
                                     @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Auth a WHERE a.tokenExpiryDate < :now AND a.status = :status")
    List<Auth> findExpiredTokens(@Param("now") LocalDateTime now, 
                                @Param("status") AuthStatus status);
} 