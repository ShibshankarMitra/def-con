package com.homedepot.supplychain.enterpriselabormanagement.exceptions;

import lombok.Getter;

@Getter
public class ElmSystemException extends RuntimeException {
    /**
     * This is a Custom Exception Class created to handle BigQuery specific System Errors like http, gateway timeouts, BigQueryException etc.
     **/
    private final String source;

    public ElmSystemException(final String source, final String message, final Throwable t) {
        super(message, t);
        this.source = source;
    }
}