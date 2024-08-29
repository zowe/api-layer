/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api.graphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.zowe.apiml.client.model.graphql.Author;
import org.zowe.apiml.client.model.graphql.Book;

import java.util.List;

@Controller
@RequestMapping("/api/v3/graphql")
public class BookController {

    @QueryMapping
    public Book bookById(@Argument String id) {
        return Book.getById(id);
    }

    @SchemaMapping
    public Author author(Book book) {
        return Author.getById(book.authorId());
    }

    @QueryMapping
    public List<Book> getAllBooks() {
        return Book.getAllBooks();
    }

    @Secured("ROLE_USER")
    @QueryMapping
    public Book getBookById(@Argument String bookId) {
        return Book.getById(bookId);
    }

    @Secured("ROLE_ADMIN")
    @MutationMapping
    public Book addBook(@Argument String name, @Argument Integer pageCount, @Argument String authorId) {
        return Book.addBook(name, pageCount, authorId);
    }

    @Secured("ROLE_ADMIN")
    @MutationMapping
    public static Book updateBook(@Argument String bookId, @Argument String name, @Argument Integer pageCount, @Argument String authorId) {
        return Book.updateBook(bookId, name, pageCount, authorId);
    }

    @Secured("ROLE_ADMIN")
    @MutationMapping
    public Book deleteBook(@Argument String bookId) {
        return Book.deleteBook(bookId);
    }

}
