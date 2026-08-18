package com.escritorio.agenda_juridica.user.dto;

import java.time.LocalDateTime;

import com.escritorio.agenda_juridica.user.UserRole;

public record UserResponse(Long id, String name, String email, UserRole role, boolean active,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
