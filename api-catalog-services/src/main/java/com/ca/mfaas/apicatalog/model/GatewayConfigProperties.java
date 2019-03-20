package com.ca.mfaas.apicatalog.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GatewayConfigProperties {

    private String scheme;
    private String hostname;

}
