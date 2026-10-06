package com.escritorio.agenda_juridica.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InitialAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void doesNothingWithoutConfiguredCredentials() {
        InitialAdminInitializer initializer =
                new InitialAdminInitializer(userRepository, passwordEncoder, "", "", "");

        initializer.run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void createsAdministratorWhenCredentialsAreConfigured() {
        when(userRepository.findByEmailIgnoreCase("admin@escritorio.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha-forte-configurada")).thenReturn("hash");
        InitialAdminInitializer initializer = new InitialAdminInitializer(
                userRepository, passwordEncoder, "Administrador", "admin@escritorio.com", "senha-forte-configurada");

        initializer.run(null);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void doesNotCreateAdministratorWithoutPassword() {
        InitialAdminInitializer initializer = new InitialAdminInitializer(
                userRepository, passwordEncoder, "Administrador", "admin@escritorio.com", "");

        initializer.run(null);

        verify(userRepository, never()).save(any(User.class));
    }
}
