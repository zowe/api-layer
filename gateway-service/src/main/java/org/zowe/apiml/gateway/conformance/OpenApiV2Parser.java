package org.zowe.apiml.gateway.conformance;

import io.swagger.models.Path;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;
import org.springframework.http.HttpMethod;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class OpenApiV2Parser extends AbstractSwaggerParser {

    private final SwaggerDeserializationResult swagger;

    OpenApiV2Parser(String swaggerDoc, Map<String, String> metadata, GatewayConfigProperties gatewayConfigProperties, String serviceId) {
        super(metadata, gatewayConfigProperties, serviceId);
        swagger = new SwaggerParser().readWithInfo(swaggerDoc);
    }

    public List<String> getMessages() {
        return swagger.getMessages();
    }

    public Set<Endpoint> getAllEndpoints() {
        HashSet<Endpoint> result = new HashSet<>();
        for (Map.Entry<String, Path> entry : swagger.getSwagger().getPaths().entrySet()) {
            HttpMethod method = getMethod(entry.getValue());
            String url = generateUrlForEndpoint(entry.getKey());
            Set<String> validResponses = getValidResponses(entry.getValue());
            Endpoint currentEndpoint = new Endpoint(url, serviceId, method, validResponses);
            result.add(currentEndpoint);
        }
        return result;
    }

    private Set<String> getValidResponses(Path value) {
        return value.getOperationMap().get(convertSpringHttpToswagger(getMethod(value))).getResponses().keySet();
    }

    private String generateUrlForEndpoint(String endpoint) {

        String baseUrl = gatewayConfigProperties.getScheme() + "://" + gatewayConfigProperties.getHostname();

        String version = searchMetadata(metadata, "apiml", "routes", "gatewayUrl");
        String serviceUrl = searchMetadata(metadata, "apiml", "routes", "serviceUrl");

        String baseEndpointPath = swagger.getSwagger().getBasePath();

        String endOfUrl;
        if (endpoint.contains("/api/")) {
            endOfUrl = serviceUrl + baseEndpointPath + endpoint;
        } else {
            endOfUrl = serviceUrl + version + baseEndpointPath + endpoint;
        }
        return baseUrl + endOfUrl.replace("//", "/");
    }

    private HttpMethod getMethod(Path value) {
        if (value.getGet() != null) {
            return HttpMethod.GET;
        } else if (value.getHead() != null) {
            return HttpMethod.HEAD;
        } else if (value.getOptions() != null) {
            return HttpMethod.OPTIONS;
        } else if (value.getPatch() != null) {
            return HttpMethod.PATCH;
        } else if (value.getPost() != null) {
            return HttpMethod.POST;
        } else if (value.getDelete() != null) {
            return HttpMethod.DELETE;
        } else if (value.getPut() != null) {
            return HttpMethod.PUT;
        }
        return null;
    }


    private io.swagger.models.HttpMethod convertSpringHttpToswagger(HttpMethod input) {
        if (input == HttpMethod.GET) {
            return io.swagger.models.HttpMethod.GET;
        } else if (input == HttpMethod.HEAD) {
            return io.swagger.models.HttpMethod.HEAD;
        } else if (input == HttpMethod.OPTIONS) {
            return io.swagger.models.HttpMethod.OPTIONS;
        } else if (input == HttpMethod.PATCH) {
            return io.swagger.models.HttpMethod.PATCH;
        } else if (input == HttpMethod.POST) {
            return io.swagger.models.HttpMethod.POST;
        } else if (input == HttpMethod.DELETE) {
            return io.swagger.models.HttpMethod.DELETE;
        } else if (input == HttpMethod.PUT) {
            return io.swagger.models.HttpMethod.PUT;
        }
        return null;
    }
}
