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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;

/**
 * Interface for a classpath entry. Implementations must be thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface JavaArchive {
    /**
     * Finds a resource in the classpath entry.
     *
     * @param s the name of the resource to find.
     * @return the resource if found, <code>null</code> otherwise.
     */
    ClasspathResource findResource(final String s);

    /**
     * The URL to the classpath entry.
     *
     * @return the URl to the classpath entry.
     */
    URL getURL();

    /**
     * Returns all classes  in the java archive.
     *
     * @return all classes.
     */
    Collection<String> getClasses();

    Collection<Class<?>> annotatedWith(Class<? extends Annotation>... annotations);

    interface InputStreamSupplier {
        InputStream get() throws IOException;
    }
}
