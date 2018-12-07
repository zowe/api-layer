package io.apiml.security.gateway.login;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GatewayLoginRequest {
    private String username;
    private String password;
}
