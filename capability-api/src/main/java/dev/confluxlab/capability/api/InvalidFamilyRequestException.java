package dev.confluxlab.capability.api;

public class InvalidFamilyRequestException extends RuntimeException {

    public InvalidFamilyRequestException() {
        super("Invalid family request.");
    }
}
