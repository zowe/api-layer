/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.zowe.apiml.caching.model.KeyValue;
import org.zowe.apiml.caching.service.Storage;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.zaas.cache.CachingClient;
import org.zowe.apiml.zaas.cache.CachingServiceClient;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InMemoryCachingClient implements CachingClient {
    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStorePath;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private char[] keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    private final Storage storage;

    private X509Certificate certificate;

    @PostConstruct
    void setup() throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        KeyStore keyStore = SecurityUtils.loadKeyStore(keyStoreType, keyStorePath, keyStorePassword);
        certificate = (X509Certificate) keyStore.getCertificate(keyAlias);
    }

    @Override
    public void create(CachingServiceClient.KeyValue kv) {

        var serviceId = getServiceId();
        storage.create(serviceId, new KeyValue(kv.getKey(), kv.getValue()));
    }

    @Override
    public void appendList(String mapKey, CachingServiceClient.KeyValue kv) {
        storage.storeMapItem(getServiceId(), mapKey, convert(kv));
    }

    @Override
    public Map<String, Map<String, String>> readAllMaps() {
        return storage.getAllMaps(getServiceId());
    }

    @Override
    public void evictTokens(String key) {
        storage.removeNonRelevantTokens(getServiceId(), key);
    }

    @Override
    public void evictRules(String key) {
        storage.removeNonRelevantRules(getServiceId(), key);
    }

    @Override
    public CachingServiceClient.KeyValue read(String key) {
        return convert(storage.read(getServiceId(), key));
    }

    @Override
    public void update(CachingServiceClient.KeyValue kv) {
        storage.update(getServiceId(), convert(kv));
    }

    @Override
    public void delete(String key) {
        storage.delete(getServiceId(), key);
    }

    KeyValue convert(CachingServiceClient.KeyValue kv) {
        return new KeyValue(kv.getKey(), kv.getValue());
    }

    CachingServiceClient.KeyValue convert(KeyValue kv) {
        return new CachingServiceClient.KeyValue(kv.getKey(), kv.getValue());
    }

    String getServiceId() {
        return certificate.getSubjectDN().getName();
    }
}
