package com.escritorio.agenda_juridica.auth;

import com.escritorio.agenda_juridica.auth.dto.AuthenticatedUserResponse;
import com.escritorio.agenda_juridica.auth.dto.LoginRequest;
import com.escritorio.agenda_juridica.auth.dto.UpdateAccountRequest;
import com.escritorio.agenda_juridica.security.AuthenticatedUser;
import com.escritorio.agenda_juridica.user.User;
import com.escritorio.agenda_juridica.user.UserRole;
import com.escritorio.agenda_juridica.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userService = userService;
    }

    public AuthenticatedUserResponse login(LoginRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
        HttpSession existingSession = httpRequest.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        saveInSession(authentication, httpRequest, httpResponse);
        return response((AuthenticatedUser) authentication.getPrincipal());
    }

    public AuthenticatedUserResponse me(AuthenticatedUser user) {
        return response(user);
    }

    /** Atualiza e-mail/senha do usuário logado e mantém a sessão com os dados novos. */
    public AuthenticatedUserResponse updateAccount(AuthenticatedUser current, UpdateAccountRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User user = userService.updateOwnAccount(current.id(), request.email(), request.password());
        AuthenticatedUser principal = AuthenticatedUser.from(user);
        saveInSession(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities()),
                httpRequest, httpResponse);
        return response(principal);
    }

    private void saveInSession(Authentication authentication, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }

    private AuthenticatedUserResponse response(AuthenticatedUser user) {
        String roleName = user.authorities().iterator().next().getAuthority().substring("ROLE_".length());
        return new AuthenticatedUserResponse(user.id(), user.name(), user.email(), UserRole.valueOf(roleName));
    }
}
