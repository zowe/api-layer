package org.zowe.apiml.apicatalog.services.status.model;

/**
 * Exception thrown when API diff is not available
 */
public class ApiDiffNotAvailableException extends RuntimeException {

    private static final long serialVersionUID = -7445346342573348213L;

    public ApiDiffNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiDiffNotAvailableException(String s){
        super(s);
    }
}
