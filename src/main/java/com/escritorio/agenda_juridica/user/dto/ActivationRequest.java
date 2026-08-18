package com.escritorio.agenda_juridica.user.dto;

import jakarta.validation.constraints.NotNull;

public record ActivationRequest(@NotNull Boolean active) {
}
