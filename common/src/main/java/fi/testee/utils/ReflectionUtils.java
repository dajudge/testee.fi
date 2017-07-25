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
}
