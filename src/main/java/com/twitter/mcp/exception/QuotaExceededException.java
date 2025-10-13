package com.twitter.mcp.exception;

/**
 * Exception thrown when API quota is exceeded.
 */
public class QuotaExceededException extends Exception {
    
    public QuotaExceededException(String message) {
        super(message);
    }
    
    public QuotaExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}

