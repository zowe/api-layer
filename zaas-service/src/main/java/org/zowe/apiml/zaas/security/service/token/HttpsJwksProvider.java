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
import org.jose4j.http.Get;
import org.jose4j.jwk.HttpsJwks;
import org.springframework.stereotype.Component;
import org.zowe.apiml.product.web.HttpConfig;

@Component
@RequiredArgsConstructor
public class HttpsJwksProvider {

    private final HttpConfig httpConfig;

    public HttpsJwks getFor(String url) {
        var httpsJwks = new HttpsJwks(url);
        var get = new Get();
        get.setSslSocketFactory(httpConfig.getSecureSslContextWithoutKeystore().getSocketFactory());
        get.setHostnameVerifier(httpConfig.getSecureHostnameVerifier());
        httpsJwks.setSimpleHttpGet(get);
        return httpsJwks;
    }

}
