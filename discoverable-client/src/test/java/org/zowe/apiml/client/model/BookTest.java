/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.client.exception.BookAlreadyExistsException;
import org.zowe.apiml.client.exception.BookNotFoundException;
import org.zowe.apiml.client.model.graphql.Book;


import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BookTest {

    @BeforeEach
    public void setUp() {
        // Reset the books list before each test
        Book.setBooks(new ArrayList<>(List.of(
            new Book("book-1", "Effective Java", 0, "author-1"),
            new Book("book-2", "Hitchhiker's Guide to the Galaxy", 208, "author-2"),
            new Book("book-3", "Down Under", 436, "author-3"))
        ));
    }

    @Test
    public void testGetAllBooks() {
        var books = Book.getAllBooks();
        assertEquals(3, books.size());
    }

    @Test
    public void testGetBookByIdFound() {
        var book = Book.getBookById("book-1");
        assertNotNull(book);
        assertEquals("Effective Java", book.name());
    }

    @Test
    public void testGetBookByIdNotFound() {
        var book = Book.getBookById("non-existent-id");
        assertNull(book);
    }

    @Test
    public void testAddBookSuccess() {
        var book = Book.addBook("New Book", 100, "author-1");
        assertNotNull(book);
        assertEquals("New Book", book.name());
        assertEquals(100, book.pageCount());
        assertTrue(Book.getAllBooks().stream().anyMatch(b -> b.bookId().equals(book.bookId())));
    }

    @Test
    public void testAddBookFailure() {
        assertThrows(BookAlreadyExistsException.class, () -> {
            Book.addBook("Effective Java", 0, "author-1");
        });
    }

    @Test
    public void testUpdateBookSuccess() {
        var updatedBook = Book.updateBook("book-1", "Updated Book", 200, "author-1");
        assertEquals("Updated Book", updatedBook.name());
        assertEquals(200, updatedBook.pageCount());
    }

    @Test
    public void testUpdateBookWithNullFieldsSuccess() {
        var updatedBook = Book.updateBook("book-1",null, null, "author-1");
        assertEquals("Updated Book", updatedBook.name());
        assertEquals(200, updatedBook.pageCount());
    }

    @Test
    public void testUpdateBookNotFound() {
        assertThrows(BookNotFoundException.class, () -> {
            Book.updateBook("non-existent-id", "Updated Book", 200, "author-1");
        });
    }

    @Test
    public void testDeleteBookSuccess() {
        var book = Book.deleteBook("book-1");
        assertNotNull(book);
        assertTrue(Book.getAllBooks().stream().noneMatch(b -> b.bookId().equals(book.bookId())));
    }

    @Test
    public void testDeleteBookNotFound() {
        assertThrows(BookNotFoundException.class, () -> {
            Book.deleteBook("non-existent-id");
        });
    }
}
