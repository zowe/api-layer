package org.zowe.apiml.cloudgatewayservice.service;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.SneakyThrows;

import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;

public class WebClientHelper {

    @SneakyThrows
    public static SslContext load(String keystorePath, char[] password) {
        File keyStoreFile = new File(keystorePath);
        if (keyStoreFile.exists()) {
            try (InputStream is = Files.newInputStream(Paths.get(keystorePath))) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(is, password);
                List<String> aliases = Collections.list(keyStore.aliases());
                return initSslContext(keyStore, password);

            }
        } else {
            throw new IllegalArgumentException("Not existing file: " + keystorePath);
        }
    }

    @SneakyThrows
    private static SslContext initSslContext(KeyStore keyStore, char[] password) {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
        kmf.init(keyStore, password);

        return SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .keyManager(kmf).build();
    }


}
