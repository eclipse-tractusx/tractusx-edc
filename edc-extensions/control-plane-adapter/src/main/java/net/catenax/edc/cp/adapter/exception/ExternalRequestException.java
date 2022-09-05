package net.catenax.edc.cp.adapter.exception;

public class ExternalRequestException extends RuntimeException {
    public ExternalRequestException(String message) {
        super(message);
    }

    public ExternalRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
