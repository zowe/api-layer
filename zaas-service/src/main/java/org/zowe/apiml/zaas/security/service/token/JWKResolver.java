/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.token;

import lombok.RequiredArgsConstructor;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JWKResolver {

    private final HttpsJwksProvider provider;

    public JsonWebKeySet resolve(String url) throws JoseException, IOException {
        var httpsJwks = provider.getFor(url);
        return new JsonWebKeySet(httpsJwks.getJsonWebKeys());
    }

}
