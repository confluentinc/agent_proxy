package io.confluent.pas.agent.proxy.security.jwt;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Validator to check if the JWT token contains the required audience.
 */
@Slf4j
@AllArgsConstructor
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final static OAuth2Error ERROR = new OAuth2Error("invalid_token", "The required audience is missing", null);

    private final String audience;

    /**
     * Validates the JWT token to ensure it contains the required audience.
     *
     * @param jwt the JWT token to validate.
     * @return the result of the validation.
     */
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience().contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        } else {
            log.error("Invalid audience: expected {}, but got {}", audience, jwt.getAudience());
            return OAuth2TokenValidatorResult.failure(ERROR);
        }
    }
}