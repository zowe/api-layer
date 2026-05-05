/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.*;

class ApimlConfTest {

    @Test
    void testDefaultValues() {
        ApimlConf conf = new ApimlConf();
        new CommandLine(conf).parseArgs();

        assertEquals("PKCS12", conf.getKeyStoreType());
        assertEquals("PKCS12", conf.getTrustStoreType());
        assertEquals("TLSv1.2", conf.getTlsVersion());
        assertNull(conf.getKeyStore());
        assertNull(conf.getTrustStore());
        assertNull(conf.getKeyPasswd());
        assertNull(conf.getTrustPasswd());
        assertNull(conf.getKeyAlias());
        assertNull(conf.getRemoteUrl());
        assertNull(conf.getRequiredHostNames());
        assertFalse(conf.isHelpRequested());
        assertFalse(conf.isDoLocalHandshake());
        assertFalse(conf.isClientCertAuth());
    }

    @Test
    void testTrustPasswdFallsBackToKeyPasswd() {
        ApimlConf conf = new ApimlConf();
        new CommandLine(conf).parseArgs("--keypasswd", "mykeypass");

        assertEquals("mykeypass", conf.getKeyPasswd());
        assertEquals("mykeypass", conf.getTrustPasswd());
    }

    @Test
    void testTrustPasswdOverridesKeyPasswd() {
        ApimlConf conf = new ApimlConf();
        new CommandLine(conf).parseArgs("--keypasswd", "mykeypass", "--trustpasswd", "mytrustpass");

        assertEquals("mykeypass", conf.getKeyPasswd());
        assertEquals("mytrustpass", conf.getTrustPasswd());
    }

    @Test
    void testTrustStoreTypeFallsBackToKeyStoreType() {
        ApimlConf conf = new ApimlConf();
        new CommandLine(conf).parseArgs("--keystoretype", "JKS");

        assertEquals("JKS", conf.getKeyStoreType());
        assertEquals("JKS", conf.getTrustStoreType());
    }

    @Test
    void testTrustStoreTypeOverridesKeyStoreType() {
        ApimlConf conf = new ApimlConf();
        new CommandLine(conf).parseArgs("--keystoretype", "JKS", "--truststoretype", "PKCS12");

        assertEquals("JKS", conf.getKeyStoreType());
        assertEquals("PKCS12", conf.getTrustStoreType());
    }

    @Test
    void testAllOptionsParsedCorrectly() {
        ApimlConf conf = new ApimlConf();
        new CommandLine(conf).parseArgs(
            "--keystore", "ks.p12",
            "--truststore", "ts.p12",
            "--keypasswd", "kpass",
            "--trustpasswd", "tpass",
            "--keystoretype", "JKS",
            "--truststoretype", "JCEKS",
            "--keyalias", "myalias",
            "--remoteurl", "https://example.com",
            "--local",
            "--help",
            "--clientcert",
            "--hostnames", "host1,host2",
            "--tlsversion", "TLSv1.3"
        );

        assertEquals("ks.p12", conf.getKeyStore());
        assertEquals("ts.p12", conf.getTrustStore());
        assertEquals("kpass", conf.getKeyPasswd());
        assertEquals("tpass", conf.getTrustPasswd());
        assertEquals("JKS", conf.getKeyStoreType());
        assertEquals("JCEKS", conf.getTrustStoreType());
        assertEquals("myalias", conf.getKeyAlias());
        assertEquals("https://example.com", conf.getRemoteUrl());
        assertTrue(conf.isDoLocalHandshake());
        assertTrue(conf.isHelpRequested());
        assertTrue(conf.isClientCertAuth());
        assertArrayEquals(new String[]{"host1", "host2"}, conf.getRequiredHostNames());
        assertEquals("TLSv1.3", conf.getTlsVersion());
    }
}