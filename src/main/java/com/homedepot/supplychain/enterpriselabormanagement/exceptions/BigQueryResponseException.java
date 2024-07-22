package com.homedepot.supplychain.enterpriselabormanagement.exceptions;

public class BigQueryResponseException extends RuntimeException {
    public BigQueryResponseException(String message) {
        super(message);
    }
}
