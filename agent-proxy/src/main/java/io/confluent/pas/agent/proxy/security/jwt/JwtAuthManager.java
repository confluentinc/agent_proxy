package io.confluent.pas.agent.proxy.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import reactor.core.publisher.Mono;

/**
 * Reactive authentication manager for JWT-based authentication.
 */
@Slf4j
public class JwtAuthManager implements ReactiveAuthenticationManager {

    private final JwtAuthenticationProvider authenticationProvider;

    /**
     * Constructor to initialize the JwtAuthManager with a JwtDecoder.
     *
     * @param decoder the JwtDecoder used to decode and validate JWT tokens.
     */
    public JwtAuthManager(JwtDecoder decoder) {
        authenticationProvider = new JwtAuthenticationProvider(decoder);
    }

    /**
     * Authenticates the given authentication token.
     *
     * @param authentication the authentication token to authenticate.
     * @return a Mono emitting the authenticated Authentication object.
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if (authentication instanceof BearerTokenAuthenticationToken) {
            return Mono.just(authenticationProvider.authenticate(authentication))
                    .doOnSuccess(auth -> {
                        log.info("Authenticated user: {}", auth.getName());
                    });
        }

        return Mono.error(new UnsupportedOperationException("Unsupported authentication type: " + authentication.getClass()));
    }
}