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
package fi.testee.classpath;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

/**
 * Base class for real java archives.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
abstract class AbstractBaseJavaArchive implements JavaArchive {
    private Collection<String> classes;
    private final AnnotationScanner annotationScanner;

    protected AbstractBaseJavaArchive(final URL... urls) {
        this.annotationScanner = new AnnotationScanner(urls);
    }

    @Override
    public final synchronized ClasspathResource findResource(final String s) {
        final Callback<ClasspathResource> cb = (is, name) -> {
            if (name.equals(s)) {
                return new ClasspathResource(is);
            }
            return null;
        };
        return iterate(cb);
    }

    @Override
    public final synchronized Collection<String> getClasses() {
        if (classes != null) {
            return classes;
        }
        classes = new HashSet<>();
        iterate((is, name) -> {
            if (name.endsWith(".class")) {
                classes.add(StringUtils.removeEnd(name.replace("/", "."), ".class"));
            }
            return null;
        });
        return classes;
    }

    protected abstract <T> T iterate(Callback<T> cb);

    protected interface Callback<T> {
        T item(InputStreamSupplier zipInputStream, String name) throws IOException;
    }

    @Override
    public Collection<Class<?>> annotatedWith(final Class<? extends Annotation>[] annotations) {
        return annotationScanner.scanFor(annotations);
    }
}
