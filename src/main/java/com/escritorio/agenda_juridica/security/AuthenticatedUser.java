package com.escritorio.agenda_juridica.security;

import java.util.Collection;
import java.util.List;

import com.escritorio.agenda_juridica.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUser(Long id, String name, String email, String password, boolean active,
        Collection<? extends GrantedAuthority> authorities) implements UserDetails {

    public static AuthenticatedUser from(User user) {
        return new AuthenticatedUser(user.getId(), user.getName(), user.getEmail(), user.getPasswordHash(),
                user.isActive(), List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public boolean isEnabled() { return active; }
}
