package com.ca.mfaas.utils.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZosmfServiceConfiguration {
    private String scheme;
    private String host;
    private int port;
}
