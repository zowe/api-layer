package org.zowe.apiml.security.common.token;

public interface AccessTokenProvider {

    int invalidateToken(String token);
}
