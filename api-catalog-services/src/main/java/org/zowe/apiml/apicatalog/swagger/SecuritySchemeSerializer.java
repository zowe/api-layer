/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class SecuritySchemeSerializer extends JsonSerializer<SecurityScheme> {


    @Override
    public void serialize(SecurityScheme value, JsonGenerator jGen, SerializerProvider serializers) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        String securitySchemeContent = objectMapper.writeValueAsString(value);

        ObjectNode jsonNode = objectMapper.readValue(securitySchemeContent, ObjectNode.class);
        SecurityScheme.Type securitySchemeType = value.getType();
        if (securitySchemeType != null) {
            jsonNode.put("type",  securitySchemeType.toString());
        }

        SecurityScheme.In securitySchemeIn = value.getIn();
        if (securitySchemeIn != null) {
            jsonNode.put("in",  securitySchemeIn.toString());
        }

        jGen.writeObject(jsonNode);
    }
}
