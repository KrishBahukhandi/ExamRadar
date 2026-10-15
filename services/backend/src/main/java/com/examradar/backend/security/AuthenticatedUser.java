package com.examradar.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String role;
    private final boolean isAdmin;

    public AuthenticatedUser(UUID id, String email, String role, boolean isAdmin) {
        this.id = id;
        this.email = email;
        this.role = role != null ? role : "authenticated";
        this.isAdmin = isAdmin;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return isAdmin; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (isAdmin) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() { return ""; }

    @Override
    public String getUsername() { return id.toString(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
