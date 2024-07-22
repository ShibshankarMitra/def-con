package com.homedepot.supplychain.enterpriselabormanagement.exceptions;

import lombok.Getter;

@Getter
public class ElmBusinessException extends RuntimeException {
    /**
     * This Custom Exception Class is a Wrapper Exception class to handle all Business Exceptions with Custom Messages.
     */
    private final String messageId;

    public ElmBusinessException(final String messageId, final String errorMessage, final Throwable t) {
        super(errorMessage, t);
        this.messageId = messageId;
    }
}