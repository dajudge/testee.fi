package com.dajudge.testee.utils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public final class AnnotationUtils {
    private AnnotationUtils() {
    }

    public static <T extends Annotation> List<T> collectAnnotations(
            final Class<?> clazz,
            final Class<T> annotation
    ) {
        final List<T> ret = new ArrayList<>();
        final T[] annotations = clazz.getAnnotationsByType(annotation);
        if (annotations != null) {
            ret.addAll(asList(annotations));
        }
        if (clazz.getSuperclass() != null) {
            ret.addAll(collectAnnotations(clazz.getSuperclass(), annotation));
        }
        return ret;
    }
}
