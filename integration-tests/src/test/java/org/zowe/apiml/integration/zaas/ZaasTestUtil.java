/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.zaas;

import lombok.experimental.UtilityClass;
import org.bouncycastle.operator.OperatorCreationException;
import org.junit.jupiter.params.provider.Arguments;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.IOException;
import java.net.URI;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.zowe.apiml.util.SecurityUtils.getClientCertificate;
import static org.zowe.apiml.util.SecurityUtils.getDummyClientCertificate;
import static org.zowe.apiml.util.requests.Endpoints.*;

@UtilityClass
public class ZaasTestUtil {

    static final URI ZAAS_TICKET_URI = HttpRequestUtils.getUriFromGateway(ZAAS_TICKET_ENDPOINT);
    static final URI ZAAS_ZOSMF_URI = HttpRequestUtils.getUriFromGateway(ZAAS_ZOSMF_ENDPOINT);
    static final URI ZAAS_ZOWE_URI = HttpRequestUtils.getUriFromGateway(ZAAS_ZOWE_ENDPOINT);

    static final URI ZAAS_SAFIDT_URI = HttpRequestUtils.getUriFromGateway(ZAAS_SAFIDT_ENDPOINT);

    static final String COOKIE = "apimlAuthenticationToken";
    static final String LTPA_COOKIE = "LtpaToken2";

    static final boolean ZOS_TARGET = Boolean.parseBoolean(System.getProperty("environment.zos.target", "false"));

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideClientCertificates() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException {
        List<Arguments> args = new ArrayList<>();
        args.add(Arguments.of(getClientCertificate()));
        if (!ZOS_TARGET) {
            args.add(Arguments.of(getDummyClientCertificate()));
        }
        return args.stream();
    }
}
