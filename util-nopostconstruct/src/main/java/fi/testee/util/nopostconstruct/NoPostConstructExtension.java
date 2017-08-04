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
package fi.testee.util.nopostconstruct;

import org.jboss.weld.util.annotated.AnnotatedTypeWrapper;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.lang.annotation.Annotation;
import java.util.Collection;

public class NoPostConstructExtension implements Extension {
    public static final NoPostConstructIntercepted ANNOTATION = new NoPostConstructIntercepted() {
        @Override
        public Class<? extends Annotation> annotationType() {
            return NoPostConstructIntercepted.class;
        }
    };
    private final Collection<Class<?>> classes;

    public NoPostConstructExtension(final Collection<Class<?>> classes) {
        this.classes = classes;
    }

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> processAnnotatedType) {
        if (!classes.contains(processAnnotatedType.getAnnotatedType().getJavaClass())) {
            return;
        }
        final AnnotatedType<T> delegate = processAnnotatedType.getAnnotatedType();
        final AnnotatedTypeWrapper<T> wrapper = new AnnotatedTypeWrapper<>(delegate, ANNOTATION);
        processAnnotatedType.setAnnotatedType(wrapper);
    }
}