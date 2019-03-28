package com.ca.mfaas.product.host;


/**
 * Exception thrown when a protocol is not http or https
 */
public class InvalidProtocolException extends RuntimeException {

    public InvalidProtocolException(String s) {
        super(s);
    }

    public InvalidProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
