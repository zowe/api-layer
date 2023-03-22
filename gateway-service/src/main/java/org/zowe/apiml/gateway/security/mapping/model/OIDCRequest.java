package org.zowe.apiml.gateway.security.mapping.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OIDCRequest {

    private String dn;
    private String registry;

}
