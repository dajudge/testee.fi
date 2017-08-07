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
package fi.testee.runtime;

import fi.testee.spi.ResourceProvider;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import java.util.HashMap;
import java.util.Map;

public class ManualResourceProviderBuilder {
    private final Map<String, Object> resources = new HashMap<>();

    public static ManualResourceProviderBuilder manualResourceProvider() {
        return new ManualResourceProviderBuilder();
    }

    public ManualResourceProviderBuilder put(final String key, final Object value) {
        resources.put(key, value);
        return this;
    }

    public ResourceProvider build() {
        return new ManualResourceProvider(resources);
    }

    private static final class ManualResourceProvider implements ResourceProvider {
        private final Map<String, Object> resources;

        public ManualResourceProvider(Map<String, Object> resources) {
            this.resources = resources;
        }

        @Override
        public Object resolve(final InjectionPoint injectionPoint) {
            if (injectionPoint.getAnnotated().getAnnotation(Resource.class) == null) {
                return null;
            }
            return resources.get(injectionPoint.getAnnotated().getAnnotation(Resource.class).mappedName());
        }

        @Override
        public Object resolve(final String jndiName, final String mappedName) {
            return null;
        }
    }
}
