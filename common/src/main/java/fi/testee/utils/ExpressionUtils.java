/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.testee.utils;

import fi.testee.exceptions.TestEEfiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utilities for expressions.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public final class ExpressionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ExpressionUtils.class);
    private static final Method evalMe = findEvalMe();

    private static Method findEvalMe() {
        try {
            final Class<?> eval = Class.forName("groovy.util.Eval");
            return eval.getMethod("me", String.class);
        } catch (final ClassNotFoundException | NoSuchMethodException e) {
            LOG.trace("Groovy not found in classpath, expression evaluation disabled.");
            return null;
        }
    }

    private ExpressionUtils() {
    }

    public static String evalExpression(final String expression) {
        if (evalMe == null) {
            return expression;
        }
        try {
            final Object result = evalMe.invoke(null, '"' + expression + '"');
            return result == null ? null : result.toString();
        } catch (final IllegalAccessException | InvocationTargetException e) {
            throw new TestEEfiException("Failed to evaluate expression with groovy", e);
        }
    }
}
