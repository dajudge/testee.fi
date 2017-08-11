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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.function.Predicate;

public final class ReflectionUtils {
    private ReflectionUtils() {
    }

    public static <T> T create(final Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new TestEEfiException("Failed to instantiate " + clazz);
        }
    }

    public static Predicate<? super Field> hasAnnotation(final Class<? extends Annotation>... annotations) {
        return field -> {
            for (final Class<? extends Annotation> annotation : annotations) {
                if (field.getAnnotation(annotation) != null) {
                    return true;
                }
            }
            return false;
        };
    }
}
