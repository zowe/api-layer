package org.zowe.apiml.gateway.conformance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.parser.SwaggerParser;
import io.swagger.v3.parser.OpenAPIV3Parser;


public class ParserFactory {
    private ParserFactory() {
    }

    public static AbstractSwaggerParser parseSwagger(String swaggerDoc) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(swaggerDoc);
        } catch (JsonProcessingException e) {
            throw new SwaggerParsingException("Could not parse Swagger documentation");
        }
        if (root.findValue("openapi") != null && root.findValue("openapi").asText().split("\\.")[0].equals("3")) {
            return new OpenApiV3Parser(new OpenAPIV3Parser().readContents(swaggerDoc));
        } else if (root.findValue("swagger") != null && root.findValue("swagger").asText().equals("2.0")) {
            return new OpenApiV2Parser(new SwaggerParser().readWithInfo(swaggerDoc));
        } else
            throw new SwaggerParsingException("Swagger documentation is not conformant to either OpenAPI V2 nor V3 - can't " +
                "find the version (that is cant find field named 'swagger' with value '2.0' or 'openapi' with version starting with '3' )");
    }
}
