package org.zowe.apiml.gateway.conformance;

import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class OpenApiV3Parser implements AbstractSwaggerParser {
    private final SwaggerParseResult swagger;


    public List<String> getMessages() {
        return swagger.getMessages();
    }


}
