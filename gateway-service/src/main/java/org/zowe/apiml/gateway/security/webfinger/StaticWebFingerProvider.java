/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.webfinger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class StaticWebFingerProvider implements WebFingerProvider {

    @Value("${apiml.security.webfinger.fileLocation:-}")
    private String webfingerDefinition;
    private static final YAMLFactory YAML_FACTORY = new YAMLFactory();

    @Override
    public WebFingerResponse getWebFingerConfig(String clientId) throws IOException {
        //set basic response
        ObjectMapper objectMapper = new ObjectMapper(YAML_FACTORY);
        WebFingerProperties webFingerProperties;
        webFingerProperties = objectMapper.readValue(new File(webfingerDefinition), WebFingerProperties.class);
        WebFingerResponse response = new WebFingerResponse();
        response.setSubject(clientId);
        response.setLinks(Collections.emptyList());

        // filter out webfinger config for given client ID
        List<WebFingerProperties.WebFingerConfig> clientConfig =
            webFingerProperties.getWebFinger().stream().filter(webFingerConfig ->
                    webFingerConfig.getClientId().equalsIgnoreCase(clientId))
                .collect(Collectors.toList());

        // update the response with stored configuration if any
        if (!clientConfig.isEmpty()) {
            List<WebFingerResponse.Link> links =
                clientConfig.stream().map(webFingerConfig ->
                        new WebFingerResponse.Link(webFingerConfig.getWellKnown()))
                    .collect(Collectors.toList());
            response.setLinks(links);
        }
        return response;
    }

    public boolean isEnabled() {
        return (webfingerDefinition != null) && !webfingerDefinition.isEmpty();
    }
}
