package com.escritorio.agenda_juridica.user;

import com.escritorio.agenda_juridica.user.dto.ActivationRequest;
import com.escritorio.agenda_juridica.user.dto.CreateUserRequest;
import com.escritorio.agenda_juridica.user.dto.UpdateUserRequest;
import com.escritorio.agenda_juridica.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Page<UserResponse> list(Pageable pageable) { return userService.list(pageable); }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) { return userService.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) { return userService.create(request); }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/activation")
    public UserResponse activate(@PathVariable Long id, @Valid @RequestBody ActivationRequest request) {
        return userService.changeActivation(id, request.active());
    }
}
