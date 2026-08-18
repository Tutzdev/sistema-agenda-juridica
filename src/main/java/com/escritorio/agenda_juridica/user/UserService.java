package com.escritorio.agenda_juridica.user;

import com.escritorio.agenda_juridica.shared.exception.BusinessRuleException;
import com.escritorio.agenda_juridica.shared.exception.ConflictException;
import com.escritorio.agenda_juridica.shared.exception.ResourceNotFoundException;
import com.escritorio.agenda_juridica.user.dto.CreateUserRequest;
import com.escritorio.agenda_juridica.user.dto.UpdateUserRequest;
import com.escritorio.agenda_juridica.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAllByOrderByNameAsc(pageable).map(this::toResponse);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return toResponse(findById(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = User.normalizeEmail(request.email());
        ensureEmailAvailable(email, null);
        User user = new User(request.name(), email, passwordEncoder.encode(request.password()), request.role());
        return toResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findById(id);
        String email = User.normalizeEmail(request.email());
        ensureEmailAvailable(email, id);
        if (user.getRole() == UserRole.ADMIN && request.role() != UserRole.ADMIN) {
            ensureAnotherActiveAdministrator(user);
        }
        String password = request.password() == null || request.password().isBlank()
                ? null : passwordEncoder.encode(request.password());
        user.update(request.name(), email, request.role(), password);
        return toResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse changeActivation(Long id, boolean active) {
        User user = findById(id);
        if (!active && user.isActive() && user.getRole() == UserRole.ADMIN) {
            ensureAnotherActiveAdministrator(user);
        }
        user.setActive(active);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public User findActiveById(Long id) {
        User user = findById(id);
        if (!user.isActive()) {
            throw new BusinessRuleException("O usuário responsável está inativo.");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private void ensureEmailAvailable(String email, Long currentId) {
        boolean duplicate = currentId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, currentId);
        if (duplicate) {
            throw new ConflictException("Já existe um usuário com este e-mail.");
        }
    }

    private void ensureAnotherActiveAdministrator(User user) {
        if (user.isActive() && userRepository.countByRoleAndActiveTrue(UserRole.ADMIN) <= 1) {
            throw new BusinessRuleException("O último administrador ativo não pode ser desativado ou rebaixado.");
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
