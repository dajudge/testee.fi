package fi.testee.utils;

import groovy.util.Eval;

/**
 * Utilities for expressions.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public final class ExpressionUtils {
    private ExpressionUtils() {
    }

    public static String evalExpression(final String expression) {
        final Object result = Eval.me('"' + expression + '"');
        return result == null ? null : result.toString();
    }
}
