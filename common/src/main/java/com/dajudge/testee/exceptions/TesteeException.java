package com.dajudge.testee.exceptions;

/**
 * Base exception for all exceptions thrown in TestEE.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TesteeException extends RuntimeException {
    /**
     * Constructor.
     */
    public TesteeException() {
    }

    /**
     * Constructor.
     *
     * @param message the detail message.
     */
    public TesteeException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public TesteeException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
