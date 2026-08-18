package com.escritorio.agenda_juridica.user;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.escritorio.agenda_juridica.shared.exception.BusinessRuleException;
import com.escritorio.agenda_juridica.shared.exception.ConflictException;
import com.escritorio.agenda_juridica.user.dto.CreateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);
        CreateUserRequest request = new CreateUserRequest("Ana", " ANA@EXAMPLE.COM ", "senha-segura", UserRole.USER);

        assertThrows(ConflictException.class, () -> userService.create(request));
    }

    @Test
    void doesNotDeactivateLastActiveAdministrator() {
        User admin = new User("Admin", "admin@example.com", "hash", UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndActiveTrue(UserRole.ADMIN)).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> userService.changeActivation(1L, false));
    }
}
