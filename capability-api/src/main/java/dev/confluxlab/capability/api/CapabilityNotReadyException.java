package dev.confluxlab.capability.api;

public class CapabilityNotReadyException extends RuntimeException {

    public CapabilityNotReadyException() {
        super("Family request operations are not implemented yet.");
    }
}
