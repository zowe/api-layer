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

import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * CLI argument parser backed by picocli.
 * Maps command-line flags (e.g. {@code --zosmf-host}, {@code --verify-certificates})
 * to configuration properties exposed via {@link ZosmfJwtCheckConfig}.
 */
@CommandLine.Command(
    name = "zosmf-jwt-check",
    version = {
        "z/OSMF JWT Check 1.0",
        "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
        "OS: ${os.name} ${os.version} ${os.arch}"
    },
    description = "Checks connectivity to the z/OSMF JWK endpoint."
)
public class ZosmfJwtCheckConf implements ZosmfJwtCheckConfig {

    @Option(names = {"--zosmf-host"}, required = true, description = "Hostname or IP of the z/OSMF server")
    private String zosmfHost;

    @Option(names = {"--zosmf-port"}, required = true, description = "Port of the z/OSMF server")
    private int zosmfPort;

    @Option(names = {"--scheme"}, description = "http or https (default: ${DEFAULT-VALUE})")
    private String scheme = "https";

    @Option(names = {"--keystore-file"}, description = "Path to the keystore file (for HTTPS mutual TLS)")
    private String keyStore;

    @Option(names = {"--keystore-password"}, arity = "0..1", interactive = true, description = "Password for the keystore")
    private String keyStorePassword;

    @Option(names = {"--keystore-type"}, description = "Type of keystore (default: ${DEFAULT-VALUE})")
    private String keyStoreType = "PKCS12";

    @Option(names = {"--truststore-file"}, description = "Path to the truststore file (for HTTPS)")
    private String trustStore;

    @Option(names = {"--truststore-password"}, arity = "0..1", interactive = true, description = "Password for the truststore")
    private String trustStorePassword;

    @Option(names = {"--truststore-type"}, description = "Type of truststore (default: ${DEFAULT-VALUE})")
    private String trustStoreType = "PKCS12";

    @Option(names = {"--verify-certificates"}, description = "Certificate verification mode: STRICT, NONSTRICT, or DISABLED (default: ${DEFAULT-VALUE})")
    private String verifyCertificates = "STRICT";

    @Option(names = {"-v", "--verbose"}, description = "Print the response body from the endpoint")
    private boolean verbose = false;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;

    @Override
    public String getZosmfHost() {
        return zosmfHost;
    }

    @Override
    public int getZosmfPort() {
        return zosmfPort;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getKeyStore() {
        return keyStore;
    }

    @Override
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    @Override
    public String getKeyStoreType() {
        return keyStoreType;
    }

    @Override
    public String getTrustStore() {
        return trustStore;
    }

    @Override
    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    @Override
    public String getTrustStoreType() {
        return trustStoreType;
    }

    @Override
    public String getVerifyCertificates() {
        return verifyCertificates;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public boolean isHelpRequested() {
        return helpRequested;
    }
}
