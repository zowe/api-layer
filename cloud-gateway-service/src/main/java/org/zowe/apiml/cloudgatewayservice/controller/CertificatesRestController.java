package org.zowe.apiml.cloudgatewayservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.cloudgatewayservice.service.CertificateChainService;

/**
 * This simple controller provides a public endpoint with the client certificate chain.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(CertificatesRestController.CONTROLLER_PATH)
public class CertificatesRestController {
    public static final String CONTROLLER_PATH = "/gateway/certificates";

    private final CertificateChainService certificateChainService;

    @GetMapping
    public ResponseEntity<String> getCertificates() {
        return new ResponseEntity<>(certificateChainService.getCertificatesInPEMFormat(), HttpStatus.OK);
    }
}
