package com.escritorio.agenda_juridica.auth.dto;

import com.escritorio.agenda_juridica.user.UserRole;

public record AuthenticatedUserResponse(Long id, String name, String email, UserRole role) {
}
