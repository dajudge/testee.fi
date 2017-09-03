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
package fi.testee.mocking;

import org.jboss.weld.util.annotated.ForwardingAnnotatedType;

import javax.enterprise.inject.spi.AnnotatedType;
import java.lang.annotation.Annotation;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilteringAnnotatedTypeWrapper<T> extends ForwardingAnnotatedType<T> {
    private final AnnotatedType<T> delegate;
    private final Predicate<Class<? extends Annotation>> filter;

    public FilteringAnnotatedTypeWrapper(
            final AnnotatedType<T> delegate,
            final Predicate<Class<? extends Annotation>> filter
    ) {
        this.delegate = delegate;
        this.filter = filter;
    }

    @Override
    public AnnotatedType<T> delegate() {
        return delegate;
    }


    @Override
    public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        final A ret = delegate.getAnnotation(annotationType);
        if (ret == null) {
            return null;
        }
        if (!filter.test(ret.annotationType())) {
            return null;
        }
        return ret;
    }

    @Override
    public Set<Annotation> getAnnotations() {
        return super.getAnnotations().stream()
                .filter(it -> filter.test(it.annotationType()))
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        if (!filter.test(annotationType)) {
            return false;
        }
        return super.isAnnotationPresent(annotationType);
    }
}
