package org.zowe.apiml.gateway.conformance;


import lombok.Data;
import org.springframework.http.HttpMethod;

import java.util.Set;


@Data
public class Endpoint {
    private final String url;
    private final String serviceId;
    private final HttpMethod httpMethod;
    private final Set<String> validResponses;
}
