package io.confluent.pas.agent.proxy.security;

import io.confluent.pas.agent.proxy.security.basic.UserAuthenticated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserAuthenticatedTest {

    @Mock
    private Authentication authentication;

    private UserAuthenticated userAuthenticated;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userAuthenticated = new UserAuthenticated(authentication, false);
    }

    @Test
    void testGetAuthoritiesWhenAuthenticated() {
        userAuthenticated.setAuthenticated(true);
        Collection<? extends GrantedAuthority> authorities = userAuthenticated.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("USER", authorities.iterator().next().getAuthority());
    }

    @Test
    void testGetAuthoritiesWhenNotAuthenticated() {
        userAuthenticated.setAuthenticated(false);
        Collection<? extends GrantedAuthority> authorities = userAuthenticated.getAuthorities();
        assertTrue(authorities.isEmpty());
    }

    @Test
    void testGetCredentials() {
        Object credentials = new Object();
        when(authentication.getCredentials()).thenReturn(credentials);
        assertEquals(credentials, userAuthenticated.getCredentials());
    }

    @Test
    void testGetDetails() {
        Object details = new Object();
        when(authentication.getDetails()).thenReturn(details);
        assertEquals(details, userAuthenticated.getDetails());
    }

    @Test
    void testGetPrincipal() {
        Object principal = new Object();
        when(authentication.getPrincipal()).thenReturn(principal);
        assertEquals(principal, userAuthenticated.getPrincipal());
    }

    @Test
    void testIsAuthenticated() {
        userAuthenticated.setAuthenticated(true);
        assertTrue(userAuthenticated.isAuthenticated());
        userAuthenticated.setAuthenticated(false);
        assertFalse(userAuthenticated.isAuthenticated());
    }

    @Test
    void testSetAuthenticated() {
        userAuthenticated.setAuthenticated(true);
        assertTrue(userAuthenticated.isAuthenticated());
        userAuthenticated.setAuthenticated(false);
        assertFalse(userAuthenticated.isAuthenticated());
    }

    @Test
    void testGetName() {
        String name = "user";
        when(authentication.getName()).thenReturn(name);
        assertEquals(name, userAuthenticated.getName());
    }
}