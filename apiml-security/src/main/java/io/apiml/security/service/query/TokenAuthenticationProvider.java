package io.apiml.security.service.query;

import io.apiml.security.service.authentication.ApimlAuthentication;
import io.apiml.security.service.authentication.TokenAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {
    private final GatewayQueryService gatewayQueryService;

    public TokenAuthenticationProvider(GatewayQueryService gatewayQueryService) {
        this.gatewayQueryService = gatewayQueryService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        TokenAuthentication tokenAuthentication = (TokenAuthentication) authentication;
        String token = tokenAuthentication.getCredentials();
        ApimlAuthentication apimlAuthentication = gatewayQueryService.query(token);
        apimlAuthentication.setAuthenticated(true);
        return apimlAuthentication;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return TokenAuthentication.class.isAssignableFrom(authentication);
    }
}
