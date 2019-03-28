package com.ca.mfaas.product.host.parser;


/**
 * Exception thrown when a Url is not parsable
 */
public class URLParserException extends RuntimeException {

    public URLParserException(String s) {
        super(s);
    }

    public URLParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
