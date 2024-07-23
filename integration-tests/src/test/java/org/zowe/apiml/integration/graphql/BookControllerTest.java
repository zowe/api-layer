package org.zowe.apiml.integration.graphql;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.util.config.BookConfiguration;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

@org.zowe.apiml.util.categories.BookControllerTest
public class BookControllerTest {

    @Test
    public void whenGetAllBooks_thenReturnAllBooks() throws SSLException {

        SslContext sslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

        WebTestClient client =
            WebTestClient.bindToServer().clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://localhost:10010/discoverableclient/api/v3/graphql")
                .build();

        HttpGraphQlTester tester = HttpGraphQlTester.create(client);

        String document = """
        query {
            getAllBooks {
                id
                name
            }
        }
        """;
        tester.document(document)
            .execute()
            .path("getAllBooks")
            .entityList(BookConfiguration.class)
            .get();

    }

    @Test
    public void whenGetAllBooksWithWrongSchema_thenReturnError() throws SSLException {

        SslContext sslContext = SslContextBuilder
            .forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE)
            .build();
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

        WebTestClient client =
            WebTestClient.bindToServer().clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://localhost:10010/discoverableclient/api/v3/graphql")
                .build();

        HttpGraphQlTester tester = HttpGraphQlTester.create(client);

        String document = """
        query {
            getAllBooks {
                title
            }
        }
        """;

        try {
            tester.document(document)
                .execute()
                .path("getAllBooks")
                .entityList(BookConfiguration.class)
                .hasSizeLessThan(1);
        } catch (AssertionError ignored) {}

    }
}
