package com.dajudge.testee.spi;

import java.lang.annotation.Annotation;
import java.util.Collection;

/**
 * Extends bean discovery by contribution annotations that qualify bean archives.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface QualifyingAnnotationExtension {
    /**
     * Returns the annotations that qualify a bean archive.
     *
     * @return the annotations.
     */
    Collection<Class<? extends Annotation>> getQualifyingAnnotations();
}
