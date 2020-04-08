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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SecuritySchemeSerializerTest {

    @Test
    void serialize() throws IOException {

        Writer jsonWriter = new StringWriter();
        JsonGenerator jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
        jsonGenerator.setCodec(new ObjectMapper());
        SerializerProvider provider = new ObjectMapper().getSerializerProvider();
        new SecuritySchemeSerializer().serialize(getDummyScheme(), jsonGenerator, provider);
        jsonGenerator.flush();
        assertEquals("{\"type\":\"http\",\"description\":\"desc\",\"name\":\"name\",\"$ref\":\"#/components/securitySchemes/ref\",\"in\":\"cookie\",\"scheme\":\"scheme\",\"bearerFormat\":\"format\",\"flows\":{},\"openIdConnectUrl\":\"url\",\"extensions\":{}}", jsonWriter.toString());
    }

    private SecurityScheme getDummyScheme() {
        SecurityScheme scheme = new SecurityScheme();
        scheme.type(SecurityScheme.Type.HTTP);
        scheme.description("desc");
        scheme.name("name");
        scheme.$ref("ref");
        scheme.in(SecurityScheme.In.COOKIE);
        scheme.scheme("scheme");
        scheme.bearerFormat("format");
        scheme.flows(new OAuthFlows());
        scheme.openIdConnectUrl("url");
        scheme.extensions(Collections.emptyMap());
        return scheme;
    }
}
