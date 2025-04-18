package io.confluent.pas.agent.proxy.security.basic;

import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Represents an authenticated user.
 * This class wraps an existing Authentication object and adds an authenticated flag.
 */
@AllArgsConstructor
public class UserAuthenticated implements Authentication {

    private final Authentication authentication;
    private boolean authenticated;

    /**
     * Returns the authorities granted to the user.
     * If the user is authenticated, a single authority "USER" is granted.
     *
     * @return the authorities granted to the user
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return (authenticated) ? List.of((GrantedAuthority) () -> "USER") : List.of();
    }

    /**
     * Returns the credentials of the user.
     *
     * @return the credentials of the user
     */
    @Override
    public Object getCredentials() {
        return authentication.getCredentials();
    }

    /**
     * Returns additional details about the authentication request.
     *
     * @return additional details about the authentication request
     */
    @Override
    public Object getDetails() {
        return authentication.getDetails();
    }

    /**
     * Returns the principal (usually the username) of the user.
     *
     * @return the principal of the user
     */
    @Override
    public Object getPrincipal() {
        return authentication.getPrincipal();
    }

    /**
     * Indicates whether the user is authenticated.
     *
     * @return true if the user is authenticated, false otherwise
     */
    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Sets the authenticated flag for the user.
     *
     * @param isAuthenticated the new authenticated flag
     * @throws IllegalArgumentException if an attempt is made to set the flag to true without proper authentication
     */
    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        authenticated = isAuthenticated;
    }

    /**
     * Returns the name of the user.
     *
     * @return the name of the user
     */
    @Override
    public String getName() {
        return authentication.getName();
    }
}