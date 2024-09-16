/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.graphql;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.util.http.HttpRequestUtils;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;


@org.zowe.apiml.util.categories.BookControllerTest
public class BookControllerTest {

    static HttpGraphQlTester tester;

    private Book setUpBook() {

        return new Book("New Book " + ThreadLocalRandom.current().nextInt(1, 10),
            ThreadLocalRandom.current().nextInt(100, 601),
            "dfvdb12");
    }

    @BeforeAll
    static void setUpTester() {
        String baseUrl = HttpRequestUtils.getUriFromGateway("/discoverableclient/api/v3/graphql").toString();
        SslContext sslContext = null;
        try {
            sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
        SslContext finalSslContext = sslContext;
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(finalSslContext));

        WebTestClient client =
            WebTestClient.bindToServer().clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();
        tester = HttpGraphQlTester.create(client);
    }

    @Test
    public void whenGetAllBooks_thenReturnAllBooks() throws SSLException {
        String document = """
        query {
            getAllBooks {
                bookId
                name
            }
        }
        """;

        var books = tester.document(document)
            .execute()
            .path("getAllBooks")
            .entityList(Book.class)
            .get();

        assertTrue(books.isEmpty());
    }

    @Test
    public void whenGetAllBooksWithWrongSchema_thenReturnException() throws SSLException {
        String document = """
        query {
            getAllBooks {
                title
            }
        }
        """;

        AssertionError thrown = assertThrows(AssertionError.class, () -> {
            tester.document(document)
                .execute()
                .path("getAllBooks")
                .entityList(Book.class)
                .get();
        });
        assertNotNull(thrown);
    }

    @Test
    public void whenAddBook_thenReturnAddedBook() throws SSLException {
        Book expectedBook = setUpBook();
        String addBookDocument = String.format("""
         mutation {
            addBook(name: "%s", pageCount: %d, authorId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, expectedBook.name, expectedBook.pageCount, expectedBook.authorId );

        tester.document(addBookDocument)
            .execute()
            .path("addBook")
            .entity(Book.class)
            .satisfies(book -> {
                assertNotNull(book.bookId);
                assertEquals(expectedBook.name, book.name);
                assertEquals(expectedBook.pageCount, book.pageCount);
            });

        String getAllBooksDocument = """
        query {
            getAllBooks {
                bookId
                name
                pageCount
            }
        }
        """;
        HttpGraphQlTester.Response getAllBooksResponse = tester.document(getAllBooksDocument)
            .execute();
        List<Book> books = getAllBooksResponse.path("getAllBooks").entityList(Book.class).get();

        assertTrue(books.stream().anyMatch(book ->
                book.name.equals(expectedBook.name) &&
                book.pageCount.equals(expectedBook.pageCount)
        ));
    }

    @Test
    public void whenGetBookById_thenReturnMatchingBook() throws SSLException {
        Book expectedBook = setUpBook();
        String addBookDocument = String.format("""
         mutation {
            addBook(name: "%s", pageCount: %d, authorId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, expectedBook.name, expectedBook.pageCount, expectedBook.authorId);
        HttpGraphQlTester.Response addBookResponse = tester.document(addBookDocument)
            .execute();
        String addedBookId = addBookResponse.path("addBook.bookId").entity(String.class).get();
        String getBookByIdDocument = String.format("""
        query {
            getBookById(bookId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, addedBookId);

        tester.document(getBookByIdDocument)
            .execute()
            .path("getBookById")
            .entity(Book.class)
            .satisfies(book -> {
                assertEquals(addedBookId, book.bookId);
            });
    }

    @Test
    public void whenGetBookByIdWithWrongId_thenBookNotFound() throws SSLException {
        String id = "UnexistingId";
        String getBookByIdDocument = String.format("""
        query {
            getBookById(bookId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, id);

        tester.document(getBookByIdDocument)
            .execute()
            .path("getBookById")
            .valueIsNull();
    }

    @Test
    public void whenAddBookWithNullParameter_thenReturnException() throws SSLException {
        Book expectedBook = setUpBook();
        String addBookDocument = String.format("""
         mutation {
            addBook(name: null, pageCount: %d, authorId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, expectedBook.pageCount, expectedBook.authorId );

        AssertionError thrown = assertThrows(AssertionError.class, () -> {
            tester.document(addBookDocument)
                .execute()
                .path("addBook")
                .entity(Book.class)
                .get();
        });
        assertNotNull(thrown);
    }

    @Test
    public void whenUpdateBook_thenReturnUpdatedBook() throws SSLException {
        Book expectedBook = setUpBook();
        String addBookDocument = String.format("""
         mutation {
            addBook(name: "%s", pageCount: %d, authorId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, expectedBook.name, expectedBook.pageCount, expectedBook.authorId);

        HttpGraphQlTester.Response addBookResponse = tester.document(addBookDocument)
            .execute();
        String addedBookId = addBookResponse.path("addBook.bookId").entity(String.class).get();
        Book updatedBook = new Book(addedBookId, "New name", 500);
        String updateBookDocument = String.format("""
        mutation {
            updateBook(bookId: "%s", name: "%s", pageCount: %d) {
                bookId
                name
                pageCount
            }
        }
        """, updatedBook.bookId, updatedBook.name, updatedBook.pageCount);

        tester.document(updateBookDocument)
            .execute()
            .path("updateBook")
            .entity(Book.class)
            .satisfies(book -> {
                assertEquals(updatedBook.bookId, book.bookId);
                assertEquals(updatedBook.name, book.name);
                assertEquals(updatedBook.pageCount, book.pageCount);
            });
    }

    @Test
    public void whenUpdateUnknownBook_thenReturnException() throws SSLException {
        Book bookToUpdateBook = setUpBook();
        bookToUpdateBook.bookId = "unknown-id";
        String updateBookDocument = String.format("""
         mutation {
            updateBook(bookId: "%s", name: "%s", pageCount: %d, authorId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, bookToUpdateBook.bookId, bookToUpdateBook.name, bookToUpdateBook.pageCount, bookToUpdateBook.authorId);

        AssertionError thrown = assertThrows(AssertionError.class, () -> {
            tester.document(updateBookDocument)
                .execute()
                .path("updateBook")
                .entity(Book.class)
                .get();
        });
        assertNotNull(thrown);
    }

    @Test
    public void whenDeleteBook_thenReturnDeletedBook() throws SSLException {
        // add book which will be later deleted
        Book bookToDelete = setUpBook();
        String addBookDocument = String.format("""
         mutation {
            addBook(name: "%s", pageCount: %d, authorId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, bookToDelete.name, bookToDelete.pageCount, bookToDelete.authorId );

        HttpGraphQlTester.Response addBookResponse = tester.document(addBookDocument)
            .execute();
        String addedBookId = addBookResponse.path("addBook.bookId").entity(String.class).get();

        //delete book
        String deleteBookDocument = String.format("""
         mutation {
            deleteBook(bookId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, addedBookId);
        tester.document(deleteBookDocument)
            .execute()
            .path("deleteBook")
            .entity(Book.class)
            .satisfies(book -> {
                assertEquals(addedBookId, book.bookId);
                assertEquals(bookToDelete.name, book.name);
                assertEquals(bookToDelete.pageCount, book.pageCount);
            });

        //verify that book was deleted
        String getBookByIdDocument = String.format("""
        query {
            getBookById(bookId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, addedBookId);

        tester.document(getBookByIdDocument)
            .execute()
            .path("getBookById")
            .valueIsNull();
    }

    @Test
    public void whenDeleteUnknownBook_thenReturnException() throws SSLException {
        String unknownId = "unknown-id";
        String deleteBookDocument = String.format("""
         mutation {
            deleteBook(bookId: "%s") {
                bookId
                name
                pageCount
            }
        }
        """, unknownId);

        AssertionError thrown = assertThrows(AssertionError.class, () -> {
            tester.document(deleteBookDocument)
                .execute()
                .path("deleteBook")
                .entity(Book.class)
                .get();
        });
        assertNotNull(thrown);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static private class Book {
        String bookId;
        String name;
        Integer pageCount;
        String authorId;

        public Book(String name, Integer pageCount, String authorId) {
            this.name = name;
            this.pageCount = pageCount;
            this.authorId = authorId;
        }
        public Book(String bookId, String name, Integer pageCount) {
            this.bookId = bookId;
            this.name = name;
            this.pageCount = pageCount;
        }
    }
}
