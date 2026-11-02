package com.escritorio.agenda_juridica.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.escritorio.agenda_juridica.user.InitialAdminProperties.Account;

@ExtendWith(MockitoExtension.class)
class InitialAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private InitialAdminInitializer initializer(Account admin, List<Account> admins) {
        return new InitialAdminInitializer(userRepository, passwordEncoder, new InitialAdminProperties(admin, admins));
    }

    @Test
    void doesNothingWithoutConfiguredCredentials() {
        initializer(new Account("", "", ""), null).run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test
    void createsAdministratorWhenCredentialsAreConfigured() {
        when(userRepository.findByEmailIgnoreCase("admin@escritorio.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha-forte-configurada")).thenReturn("hash");

        initializer(new Account("Administrador", "admin@escritorio.com", "senha-forte-configurada"), null).run(null);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void doesNotCreateAdministratorWithoutPassword() {
        initializer(new Account("Administrador", "admin@escritorio.com", ""), null).run(null);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createsEveryAdministratorFromTheList() {
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");

        initializer(null, List.of(
                new Account("Primeiro", "primeiro@escritorio.com", "senha-um-configurada"),
                new Account("Segundo", "segundo@escritorio.com", "senha-dois-configurada"),
                new Account("Incompleto", "incompleto@escritorio.com", ""))).run(null);

        verify(userRepository, times(2)).save(any(User.class));
    }
}
