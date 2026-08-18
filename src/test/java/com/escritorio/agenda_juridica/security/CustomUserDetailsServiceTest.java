package com.escritorio.agenda_juridica.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRepository;
import com.escritorio.agenda_juridica.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void inactiveUserCannotAuthenticate() {
        User user = new User("Ana", "ana@example.com", "hash", UserRole.USER);
        user.setActive(false);
        when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));

        UserDetails details = new CustomUserDetailsService(userRepository).loadUserByUsername("ana@example.com");

        assertFalse(details.isEnabled());
    }
}
