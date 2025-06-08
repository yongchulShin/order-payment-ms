package com.example.userservice.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String address;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UserCreateRequest {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String address;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class UserUpdateRequest {
    private String phone;
    private String address;
} 