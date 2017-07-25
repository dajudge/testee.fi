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

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import static fi.testee.utils.UrlUtils.createCompositeUrl;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * A {@link JavaArchive} merging multiple delegate archives to one.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class CompositeJavaArchive implements JavaArchive {
    private final Collection<JavaArchive> archives;
    private final URL url;

    /**
     * Constructor.
     *
     * @param archives the archives to make look like one.
     */
    public CompositeJavaArchive(final Collection<? extends JavaArchive> archives) {
        this.archives = Collections.unmodifiableCollection(archives);
        this.url = createCompositeUrl(archives.stream()
                .map(JavaArchive::getURL)
                .collect(toList())
        );
    }


    @Override
    public ClasspathResource findResource(final String s) {
        for (final JavaArchive archive : archives) {
            final ClasspathResource ret = archive.findResource(s);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public Collection<String> getClasses() {
        return archives.stream().map(JavaArchive::getClasses).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @Override
    public Collection<Class<?>> annotatedWith(Class<? extends Annotation>[] annotations) {
        return archives.stream().map(it -> it.annotatedWith(annotations)).flatMap(Collection::stream).collect(toSet());
    }

    @Override
    public String toString() {
        return "CompositeJavaArchive{" +
                "url=" + url +
                '}';
    }
}
