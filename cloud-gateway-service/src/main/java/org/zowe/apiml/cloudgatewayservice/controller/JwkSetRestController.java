package org.zowe.apiml.cloudgatewayservice.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.cloudgatewayservice.security.AuthSourceSign;

import java.security.interfaces.RSAPublicKey;

@RequiredArgsConstructor
@RestController
public class JwkSetRestController {

    private final AuthSourceSign authSourceSign;

    @GetMapping("/.well-known/jwks.json")
    @ResponseBody
    public ResponseEntity<Object> keys() {
        JWKSet publicKeys = getJwkPublicKeys();
        if (publicKeys == null) {
            return new ResponseEntity<>("No public key available.", HttpStatus.NOT_FOUND);
        } else {
            return new ResponseEntity<>(publicKeys.toJSONObject(true), HttpStatus.OK);
        }
    }


    private JWKSet getJwkPublicKeys() {
        if (authSourceSign.getPublicKey() == null) {
            return null;
        }
        final RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) authSourceSign.getPublicKey())
            .keyUse(KeyUse.SIGNATURE)
            .algorithm(JWSAlgorithm.RS256)
            .build();
        return new JWKSet(rsaKey);
    }

}
