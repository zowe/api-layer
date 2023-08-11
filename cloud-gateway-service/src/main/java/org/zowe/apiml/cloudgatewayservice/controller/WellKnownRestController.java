package org.zowe.apiml.cloudgatewayservice.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.cloudgatewayservice.security.AuthSourceSign;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping(WellKnownRestController.CONTROLLER_PATH)
public class WellKnownRestController {
    public static final String CONTROLLER_PATH = "/gateway/.well-known";

    private final AuthSourceSign authSourceSign;

    @GetMapping("/jwks.json")
    @ResponseBody
    public Map<String, Object> getPublicKeys() {
        JWKSet jwkSet = buildJWKSet();
        return jwkSet.toJSONObject(true);
    }

    private JWKSet buildJWKSet() {
        PublicKey publicKey = authSourceSign.getPublicKey();
        if (publicKey == null) {
            return new JWKSet();
        }
        final RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) publicKey)
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .build();
        return new JWKSet(rsaKey);
    }
}
