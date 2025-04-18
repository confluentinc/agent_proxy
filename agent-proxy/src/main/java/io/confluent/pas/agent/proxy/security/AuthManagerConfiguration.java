package io.confluent.pas.agent.proxy.security;

import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.proxy.security.basic.BasicAuthManager;
import io.confluent.pas.agent.proxy.security.jwt.AudienceValidator;
import io.confluent.pas.agent.proxy.security.jwt.JwtAuthManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Configuration class for setting up the authentication manager.
 * This class configures the security filter chain for the application.
 */
@Slf4j
@Configuration
@EnableWebFluxSecurity
public class AuthManagerConfiguration {

    @Value("${authentication.basic.cache-size:#{0}}")
    private int cacheSize = 100;
    @Value("${authentication.basic.cache-expiry-in-second:#{0}}")
    private int cacheExpiry = 3600;
    @Value("${authentication.enabled}")
    private boolean authenticationEnabled = true;

    @Value("${authentication.jwt.issuer-uri:#{null}}")
    private String jwtIssuerUri;
    @Value("${authentication.jwt.audience:#{null}}")
    private String audience;

    /**
     * Configures the security filter chain for the server.
     * This method sets up basic authentication and disables CSRF protection.
     *
     * @param http               the ServerHttpSecurity instance
     * @param kafkaConfiguration the kafka configuration
     * @return the configured SecurityWebFilterChain
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.server", name = "mode", havingValue = "sse")
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
                                                            KafkaConfiguration kafkaConfiguration) {
        if (!authenticationEnabled) {
            return http.authorizeExchange((exchanges) -> exchanges.anyExchange().permitAll())
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .headers(Customizer.withDefaults())
                    .build();
        }

        final ServerHttpSecurity security = http
                .authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated())
                .csrf(ServerHttpSecurity.CsrfSpec::disable);

        // If basic authentication is enabled, set up the authentication manager
        if (cacheSize > 0 && cacheExpiry > 0) {
            security.httpBasic((httpBasicSpec) -> {
                        httpBasicSpec.authenticationManager(new BasicAuthManager(kafkaConfiguration, cacheSize, cacheExpiry));
                    })
                    .formLogin((httpBasicSpec) -> {
                        httpBasicSpec.authenticationManager(new BasicAuthManager(kafkaConfiguration, cacheSize, cacheExpiry));
                    })
                    .authenticationManager(new BasicAuthManager(kafkaConfiguration, cacheSize, cacheExpiry));
        }

        // If JWT authentication is enabled, set up the authentication manager
        if (StringUtils.isNotEmpty(jwtIssuerUri)) {
            security.oauth2ResourceServer((oauth2) -> {
                oauth2.authenticationManagerResolver(
                        (exchange) -> Mono.just(new JwtAuthManager(jwtDecoder())));
            });
        }

        return security.build();
    }

    /**
     * Creates a JwtDecoder bean to decode and validate JWT tokens.
     * This method sets up the audience validator if the audience is specified.
     *
     * @return the configured JwtDecoder
     */
    private JwtDecoder jwtDecoder() {
        final NimbusJwtDecoder jwtDecoder = JwtDecoders.fromIssuerLocation(jwtIssuerUri);

        if (StringUtils.isNotEmpty(audience)) {
            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(jwtIssuerUri);
            OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

            jwtDecoder.setJwtValidator(withAudience);
        }

        return jwtDecoder;
    }
}