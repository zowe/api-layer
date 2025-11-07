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

public class ZaasTestUtil {

    private ZaasTestUtil() {
        super();
    }

    static final URI ZAAS_TICKET_URI = HttpRequestUtils.getUriFromZaas(ZAAS_TICKET_ENDPOINT);
    static final URI ZAAS_ZOSMF_URI = HttpRequestUtils.getUriFromZaas(ZAAS_ZOSMF_ENDPOINT);
    static final URI ZAAS_ZOWE_URI = HttpRequestUtils.getUriFromZaas(ZAAS_ZOWE_ENDPOINT);

    static final URI ZAAS_SAFIDT_URI = HttpRequestUtils.getUriFromZaas(ZAAS_SAFIDT_ENDPOINT);

    static final String COOKIE = "apimlAuthenticationToken";
    static final String LTPA_COOKIE = "LtpaToken2";

    static final boolean ZOS_TARGET = Boolean.parseBoolean(System.getProperty("environment.zos.target", "false"));

    public static Stream<Arguments> provideClientCertificates() throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, NoSuchProviderException, OperatorCreationException {
        List<Arguments> args = new ArrayList<>();
        args.add(Arguments.of(getClientCertificate(), "client certificate"));
        if (!ZOS_TARGET) {
            args.add(Arguments.of(getDummyClientCertificate(), "dummy client certificate"));
        }
        return args.stream();
    }

    /**
     * Some tests are written as integration tests and make the test runner act as the Gateway (they sign tokens for example, or use the server's private key to act as Gateway)
     * These tests cannot run in an environment where the server has the private key in hardware as it is not available to the runner and the validations fail because the runner
     * cannot provide valid server credentials.
     *
     * @return a boolean indicating if the test runner is working against an instance with ICSF hardware keyring
     */
    public static boolean isTestForICSF() {
        return Boolean.getBoolean("hwkeyring");
    }

}
