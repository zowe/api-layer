package org.zowe.apiml.client.model.graphql;
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

    public static Book getById(String bookId) {
        return books.stream()
            .filter(book -> book.bookId().equals(bookId))
            .findFirst()
            .orElse(null);
    }

    public static Book addBook(String name, Integer pageCount, String authorId){
        Book book = new Book(UUID.randomUUID().toString(), name, pageCount, authorId);
        books.add(book);
        return book;
    }

    public static Book updateBook(String bookId, String name, Integer pageCount, String authorId){
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
            return addBook(name, pageCount, authorId);
        }
    }

    public static Book deleteBook(String bookId){
        Book book = getById(bookId);
        books.remove(book);
        return book;
    }
}
