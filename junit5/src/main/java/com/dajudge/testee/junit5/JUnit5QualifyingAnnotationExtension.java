package com.dajudge.testee.junit5;

import com.dajudge.testee.spi.QualifyingAnnotationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static java.util.Arrays.asList;

public class JUnit5QualifyingAnnotationExtension implements QualifyingAnnotationExtension {
    @Override
    public Collection<Class<? extends Annotation>> getQualifyingAnnotations() {
        return asList(Test.class, TestFactory.class, ExtendWith.class);
    }
}
