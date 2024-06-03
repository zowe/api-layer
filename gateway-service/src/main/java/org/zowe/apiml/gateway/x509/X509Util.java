/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.x509;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.SslInfo;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

@Slf4j
@UtilityClass
public class X509Util {

    public String getEncodedClientCertificate(SslInfo sslInfo) throws CertificateEncodingException {
        if (sslInfo == null) return null;

        X509Certificate[] certificates = sslInfo.getPeerCertificates();
        if (isEmpty(certificates)) return null;

        return Base64.getEncoder().encodeToString(certificates[0].getEncoded());
    }

}
