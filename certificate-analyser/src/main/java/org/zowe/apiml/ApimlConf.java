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

import picocli.CommandLine.Option;

public class ApimlConf {

    @Option(names = {"-ks", "--keystore"}, description = "Path to keystore file or keyring")
    private String keyStore;
    @Option(names = {"-ts", "--truststore"}, description = "Path to truststore file or keyring")
    private String trustStore;
    @Option(names = {"-tpw", "--trustpasswd"}, description = "Truststore password")
    private String trustPasswd;
    @Option(names = {"-kpw", "--keypasswd"}, description = "Keystore password")
    private String keyPasswd;
    @Option(names = {"-tst", "--truststoretype"}, description = "Truststore type, i.e. PKCS12")
    private String trustStoreType;
    @Option(names = {"-kst", "--keystoretype"}, description = "Keystore type, i.e. PKCS12")
    private String keyStoreType;
    @Option(names = {"-a", "--keyalias"}, description = "Alias under which this key is stored")
    private String keyAlias;
    @Option(names = {"-r", "--remoteurl"}, description = "URL of service to be verified")
    private String remoteUrl;

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
        return defaultValue(trustStoreType,keyStoreType);
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

    private String defaultValue(String value, String defaultVal){
        return value != null ? value : defaultVal;
    }
}


