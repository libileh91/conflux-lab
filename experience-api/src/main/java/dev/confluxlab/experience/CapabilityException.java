package dev.confluxlab.experience;

import org.springframework.http.HttpStatus;

public class CapabilityException extends RuntimeException {

    private final HttpStatus status;

    public CapabilityException(HttpStatus status) {
        super(status.getReasonPhrase());
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
