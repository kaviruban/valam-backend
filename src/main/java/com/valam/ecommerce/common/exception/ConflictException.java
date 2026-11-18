package com.valam.ecommerce.common.exception;

/** The request is valid but clashes with the current state, e.g. the product was edited by someone else. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
