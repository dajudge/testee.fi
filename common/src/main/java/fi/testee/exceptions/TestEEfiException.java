package fi.testee.exceptions;

/**
 * Base exception for all exceptions thrown in TestEE.fi.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestEEfiException extends RuntimeException {
    /**
     * Constructor.
     */
    public TestEEfiException() {
    }

    /**
     * Constructor.
     *
     * @param message the detail message.
     */
    public TestEEfiException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message the detail message.
     * @param cause   the cause.
     */
    public TestEEfiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
