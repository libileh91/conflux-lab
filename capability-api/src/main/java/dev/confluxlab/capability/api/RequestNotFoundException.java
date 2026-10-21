package dev.confluxlab.capability.api;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException() {
        super("Request not found.");
    }
}
