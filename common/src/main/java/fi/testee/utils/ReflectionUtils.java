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
import java.util.Collection;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFieldsList;

public final class ReflectionUtils {
    private ReflectionUtils() {
    }

    public static Collection<Object> valuesOf(final Object instance, final Collection<Field> fields) {
        return fields.stream()
                .map(it -> {
                    it.setAccessible(true);
                    try {
                        return it.get(instance);
                    } catch (final IllegalAccessException e) {
                        throw new TestEEfiException("Could not extract field value from " + instance, e);
                    }
                }).collect(toSet());
    }

    public static Collection<Object> fieldsValuesAnnotatedWith(
            final Object o,
            final Class<? extends Annotation>... annotations
    ) {
        final Set<Field> fields = AnnotationUtils.groupByAnnotation(getAllFieldsList(o.getClass()), annotations).values().stream()
                .flatMap(Collection::stream)
                .collect(toSet());
        return valuesOf(o, fields);
    }

    public static <T> T create(final Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new TestEEfiException("Failed to instantiate " + clazz);
        }
    }
}
