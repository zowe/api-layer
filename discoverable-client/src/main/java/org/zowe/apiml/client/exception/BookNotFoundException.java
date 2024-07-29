package org.zowe.apiml.client.exception;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException() {
        super("Book has not been found");
    }
}
