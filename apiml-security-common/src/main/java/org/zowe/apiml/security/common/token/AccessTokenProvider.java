package org.zowe.apiml.security.common.token;

public interface AccessTokenProvider {

    void invalidateToken(String token) throws Exception;
    boolean isInvalidated(String token) throws Exception;
}
