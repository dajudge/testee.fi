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

import fi.testee.exceptions.TestEEfiException;
import org.scannotation.AnnotationDB;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Annotation scanning.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class AnnotationScanner {
    private final URL[] urls;
    private Map<String, Set<String>> index;

    public AnnotationScanner(final URL... urls) {
        this.urls = urls;
    }

    public Collection<Class<?>> scanFor(Class<? extends Annotation>... annotations) {
        final Collection<Class<?>> ret = new ArrayList<>();
        Arrays.stream(annotations).forEach(annotation -> {
                    final Set<String> classNames = index().get(annotation.getName());
                    if (classNames != null) {
                        classNames.forEach(clazz -> {
                                    try {
                                        ret.add(Class.forName(clazz));
                                    } catch (final ClassNotFoundException e) {
                                        throw new TestEEfiException("Failed to load class", e);
                                    }
                                }
                        );
                    }
                }
        );
        return ret;
    }

    private synchronized Map<String, Set<String>> index() {
        if (index == null) {
            final AnnotationDB db = new AnnotationDB();
            try {
                db.scanArchives(urls);
            } catch (final IOException e) {
                throw new TestEEfiException("Failed to perform annotation scanning", e);
            }
            index = db.getAnnotationIndex();

        }
        return index;
    }
}
