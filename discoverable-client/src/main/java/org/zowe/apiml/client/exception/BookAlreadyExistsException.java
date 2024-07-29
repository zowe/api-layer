package org.zowe.apiml.client.exception;

public class BookAlreadyExistsException extends RuntimeException {
    public BookAlreadyExistsException() {
        super("A book with the given name, page count, and author ID already exists.");
    }
}
