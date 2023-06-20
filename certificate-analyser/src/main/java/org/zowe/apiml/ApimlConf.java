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

@CommandLine.Command(version = {
    "Versioned Command 1.0",
    "JVM: ${java.version} (${java.vendor} ${java.vm.name} ${java.vm.version})",
    "OS: ${os.name} ${os.version} ${os.arch}"})
public class ApimlConf implements Config {

    @Option(names = {"-k", "--keystore"}, description = "Path to keystore file or keyring. When using keyring, pass -Djava.protocol.handler.pkgs=com.ibm.crypto.provider in command line.")
    private String keyStore;
    @Option(names = {"-t", "--truststore"}, description = "Path to truststore file or keyring")
    private String trustStore;
    @Option(names = {"-tp", "--trustpasswd"}, arity = "0..1", interactive = true, description = "Truststore password")
    private String trustPasswd;
    @Option(names = {"-kp", "--keypasswd"}, arity = "0..1", interactive = true, description = "Keystore password")
    private String keyPasswd;
    @Option(names = {"-tt", "--truststoretype"}, description = "Truststore type, default is PKCS12")
    private String trustStoreType;
    @Option(names = {"-kt", "--keystoretype"}, description = "Keystore type, default is PKCS12")
    private String keyStoreType = "PKCS12";
    @Option(names = {"-a", "--keyalias"}, description = "Alias under which this key is stored")
    private String keyAlias;
    @Option(names = {"-r", "--remoteurl"}, description = "URL of service to be verified")
    private String remoteUrl;
    @Option(names = {"-l", "--local"}, description = "Do SSL handshake on localhost")
    private boolean doLocalHandshake;
    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display a help message")
    private boolean helpRequested = false;
    @Option(names = {"-c", "--clientcert"}, description = "Add client certificate to HTTPS request")
    private boolean clientCertAuth;

    public String getKeyStore() {
        return keyStore;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public String getTrustPasswd() {
        return defaultValue(trustPasswd, keyPasswd);
    }

    public String getKeyPasswd() {
        return keyPasswd;
    }

    public String getTrustStoreType() {
        return defaultValue(trustStoreType, keyStoreType);
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public boolean isDoLocalHandshake() {
        return doLocalHandshake;
    }

    public boolean isClientCertAuth() {
        return clientCertAuth;
    }

    private String defaultValue(String value, String defaultVal) {
        return value != null ? value : defaultVal;
    }
}


