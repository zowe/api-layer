package io.apiml.security.service.login.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceLoginResponse {
    private String apimlAuthenticationToken;
}
