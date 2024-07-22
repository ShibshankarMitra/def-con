package com.homedepot.supplychain.enterpriselabormanagement.exceptions;

public class JsonValidationException extends RuntimeException {
    public JsonValidationException(final String errorMessage) {
        super(errorMessage);
    }
}