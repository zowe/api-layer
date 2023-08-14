package org.zowe.apiml.gateway.conformance;

import io.swagger.parser.util.SwaggerDeserializationResult;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class OpenApiV2Parser implements AbstractSwaggerParser {

    private final SwaggerDeserializationResult swagger;

    public List<String> getMessages() {
        return swagger.getMessages();
    }


}
