package com.ca.mfaas.eurekaservice.client.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ssl {
    private String enabled;
    private String keyAlias;
    private String keyPassword;
    private String keyStore;
    private String keyStorePassword;
    private String keyStoreType;
    private String trustStore;
    private String trustStorePassword;
    private String trustStoreType;
}
