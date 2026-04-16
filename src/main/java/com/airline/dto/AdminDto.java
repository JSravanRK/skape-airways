package com.airline.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Safe user DTO returned by admin endpoints — never exposes the password field.
 */
public class AdminDto {

    @Data
    public static class UserResponse {
        private Long id;
        private String name;
        private String email;
        private String role;
        private String phone;
        private int milesBalance;
        private LocalDateTime createdAt;
    }
}
