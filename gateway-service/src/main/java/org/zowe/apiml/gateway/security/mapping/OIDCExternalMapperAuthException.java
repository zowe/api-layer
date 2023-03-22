package org.zowe.apiml.gateway.security.mapping;

import lombok.experimental.Delegate;
import org.springframework.security.core.AuthenticationException;
import org.zowe.apiml.gateway.security.mapping.model.MapperResponse;

public class OIDCExternalMapperAuthException extends AuthenticationException {

    private static final long serialVersionUID = -457112306751545949L;

    @Delegate
    private final transient MapperResponse mapperResponse;

    public OIDCExternalMapperAuthException(String msg, MapperResponse mapperResponse) {
        super(msg);
        this.mapperResponse = mapperResponse;
    }

}
