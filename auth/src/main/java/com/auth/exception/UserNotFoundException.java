package com.auth.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("Authenticated user was not found.");
    }
}
