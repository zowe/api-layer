package org.zowe.apiml.security;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Builder meant to be used only for non-strict configuration
 */
public class HostnameIgnoringSSLContextBuilder extends org.apache.hc.core5.ssl.SSLContextBuilder {

    @Override
    protected void initSSLContext(SSLContext sslContext, Collection<KeyManager> keyManagers,
            Collection<TrustManager> trustManagers, SecureRandom secureRandom) throws KeyManagementException {

        var tm = trustManagers.iterator().next();
        Collection<TrustManager> laxTrustManager = new ArrayList<>();
        laxTrustManager.add(new HostnameIgnoringTrustManager((X509TrustManager) tm));
        super.initSSLContext(sslContext, keyManagers, laxTrustManager, secureRandom);
    }

    public static HostnameIgnoringSSLContextBuilder create() {
        return new HostnameIgnoringSSLContextBuilder();
    }

}
