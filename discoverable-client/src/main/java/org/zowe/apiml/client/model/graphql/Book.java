/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.model.graphql;
import org.zowe.apiml.client.exception.BookAlreadyExistsException;
import org.zowe.apiml.client.exception.BookNotFoundException;

import java.util.*;

public record Book (String bookId, String name, Integer pageCount, String authorId) {

    private static List<Book> books = new ArrayList<>(List.of(
        new Book("book-1", "Effective Java", 0, "author-1"),
        new Book("book-2", "Hitchhiker's Guide to the Galaxy", 208, "author-2"),
        new Book("book-3", "Down Under", 436, "author-3"))
    );

    public static List<Book> getAllBooks() {
        return books.stream().toList();
    }

    public static Book getBookById(String bookId) {
        return books.stream().filter(b -> b.bookId.equals(bookId)).findFirst().orElse(null);
    }

    public static Book getById(String bookId) {
        return books.stream()
            .filter(book -> book.bookId().equals(bookId))
            .findFirst()
            .orElse(null);
    }

    public static Book addBook(String name, Integer pageCount, String authorId) {
        Book bookToAdd = books.stream()
            .filter(book -> book.name().equals(name) &&
                book.pageCount().equals(pageCount) &&
                book.authorId().equals(authorId))
            .findFirst()
            .orElse(null);

        if (bookToAdd == null) {
            Book book = new Book(UUID.randomUUID().toString(), name, pageCount, authorId);
            books.add(book);
            return book;
        }
        else {
            throw new BookAlreadyExistsException();
        }
    }

    public static Book updateBook(String bookId, String name, Integer pageCount, String authorId) {
        Book bookToUpdate = books.stream()
            .filter(book -> book.bookId().equals(bookId))
            .findFirst()
            .orElse(null);

        if (bookToUpdate != null) {
            Book updatedBook = new Book(
                bookToUpdate.bookId(),
                name != null ? name : bookToUpdate.name(),
                pageCount != null ? pageCount : bookToUpdate.pageCount(),
                authorId != null ? authorId : bookToUpdate.authorId()
            );
            books.set(books.indexOf(bookToUpdate), updatedBook);
            return updatedBook;
        }
        else {
            throw new BookNotFoundException();
        }
    }

    public static Book deleteBook(String bookId) {
        Book book = getById(bookId);
        if (book != null) {
            books.remove(book);
            return book;
        }
        throw new BookNotFoundException();
    }
}
