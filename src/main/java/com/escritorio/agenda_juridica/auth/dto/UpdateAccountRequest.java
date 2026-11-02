package com.escritorio.agenda_juridica.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alteração dos próprios dados de acesso. Senha em branco mantém a atual. */
public record UpdateAccountRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @Size(min = 6, max = 100) String password) {
}
