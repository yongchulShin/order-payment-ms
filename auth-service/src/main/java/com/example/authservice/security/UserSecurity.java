package com.example.authservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {
    
    /**
     * 현재 인증된 사용자가 요청한 리소스의 소유자인지 확인합니다.
     * 
     * @param userId 접근하려는 사용자 리소스의 ID
     * @return 현재 사용자가 리소스 소유자이면 true, 아니면 false
     */
    public boolean isCurrentUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // JWT 토큰에서 추출한 사용자 ID (sub claim)와 요청한 리소스의 사용자 ID를 비교
        String currentUserId = authentication.getName();
        return userId.toString().equals(currentUserId);
    }
} 