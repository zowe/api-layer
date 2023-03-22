package org.zowe.apiml.gateway.security.mapping;

import org.springframework.security.access.AccessDeniedException;

public class OIDCExternalMapperException extends AccessDeniedException {

    private static final long serialVersionUID = 5147129144299104838L;

    public OIDCExternalMapperException(String msg) {
        super(msg);
    }

    public OIDCExternalMapperException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
