/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/api/v1/zosmf")
@RequiredArgsConstructor
public class ZosmfController {
    private final RestTemplate restTemplate;

    @GetMapping(value = "/request")
    @ApiOperation(value = "Lists catalog dashboard tiles",
        notes = "Returns a list of tiles including status and tile description"
    )
    public String request() {
        String uri = "https://usilca3x.lvn.broadcom.net:1443/zosmf/restjobs/jobs";

        String credentials = "abdil01:jeton321";
        String base64Credentials = new String(Base64.encodeBase64(credentials.getBytes()));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jobRequest = mapper.createObjectNode();
        jobRequest.put("file", "//'AICJI01.JCL(NOP)'");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-CSRF-ZOSMF-HEADER", "zosmf");
        // headers.add("X-IBM-Notification-URL", "https://aicji01w10.ca.com:10012/discoverableclient/api/v1/zosmf/response");
        headers.add("X-IBM-Notification-URL", "https://usilca3x.lvn.broadcom.net:10210/api/v1/discoverableclient/zosmf/response");
        // headers.add("X-IBM-Notification-URL", "https://ca31.lvn.broadcom.net:10213/discoverableclient/api/v1/zosmf/response");
        headers.add("Authorization", "Basic " + base64Credentials);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                uri,
                HttpMethod.PUT,
                new HttpEntity<>(jobRequest, headers),
                String.class);

            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return e.getMessage() + ": " + e.getResponseBodyAsString();
        }
    }

    @PostMapping(value = "/response")
    public String response() {
        log.info("Job ended.");

        return "ok";
    }
}
