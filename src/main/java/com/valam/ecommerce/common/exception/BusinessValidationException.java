package com.valam.ecommerce.common.exception;

/** A well-formed request that breaks a business rule, e.g. "ml" used for a powder product. */
public class BusinessValidationException extends RuntimeException {

    public BusinessValidationException(String message) {
        super(message);
    }
}
