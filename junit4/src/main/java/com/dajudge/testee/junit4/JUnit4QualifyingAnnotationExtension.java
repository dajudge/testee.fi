package com.dajudge.testee.junit4;

import com.dajudge.testee.spi.QualifyingAnnotationExtension;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static java.util.Arrays.asList;

public class JUnit4QualifyingAnnotationExtension implements QualifyingAnnotationExtension {
    @Override
    public Collection<Class<? extends Annotation>> getQualifyingAnnotations() {
        return asList(Test.class, RunWith.class);
    }
}
