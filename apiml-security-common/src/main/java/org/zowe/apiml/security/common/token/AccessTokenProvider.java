/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.token;

import java.io.IOException;
import java.util.Set;

public interface AccessTokenProvider {

    void invalidateToken(String token) throws IOException;
    boolean isInvalidated(String token);
    String getToken(String username, int expirationTime, Set<String> scopes);
    boolean isValidForScopes(String token, String serviceId);
    void invalidateAllTokensForUser(String userId, long timeStamp);
    void invalidateAllTokensForService(String serviceId, long timeStamp);
    void evictNonRelevantTokensAndRules();
}
