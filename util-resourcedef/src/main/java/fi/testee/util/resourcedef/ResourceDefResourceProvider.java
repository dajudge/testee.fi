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
package fi.testee.util.resourcedef;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.ResourceProvider;
import fi.testee.spi.scope.TestInstanceScope;

import javax.annotation.Resource;
import javax.enterprise.inject.spi.InjectionPoint;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

@TestInstanceScope
public class ResourceDefResourceProvider implements ResourceProvider {
    @Resource(mappedName = "testeefi/instance/instance")
    private Object testInstance;

    @Override
    public Object resolve(final InjectionPoint injectionPoint) {
        final Type type = injectionPoint.getType();
        final Set<Field> candidates = stream(getAllFields(testInstance.getClass()))
                .filter(field -> field.getAnnotation(ResourceDef.class) != null)
                .filter(field -> field.getType().isAssignableFrom((Class) type))
                .collect(toSet());
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() > 1) {
            throw new TestEEfiException("Ambiguous fields for injection point " + injectionPoint + " found: " + candidates);
        }
        final Field field = candidates.iterator().next();
        field.setAccessible(true);
        try {
            return field.get(testInstance);
        } catch (final IllegalAccessException e) {
            throw new TestEEfiException("Failed to retrieve @ResourceDef field value from " + field, e);
        }
    }

    @Override
    public Object resolve(final String jndiName, final String mappedName) {
        return null;
    }
}
