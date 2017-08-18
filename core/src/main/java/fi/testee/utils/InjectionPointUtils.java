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
package fi.testee.utils;

import fi.testee.exceptions.TestEEfiException;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.injection.FieldInjectionPoint;
import org.jboss.weld.injection.InjectionPointFactory;
import org.jboss.weld.manager.BeanManagerImpl;

import javax.enterprise.inject.spi.Bean;
import java.lang.reflect.Field;
import java.util.Collection;

public final class InjectionPointUtils {
    private InjectionPointUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> FieldInjectionPoint<Object, T> injectionPointOf(
            final Field field,
            final Bean<T> bean,
            final BeanManagerImpl beanManager
    ) {
        final EnhancedAnnotatedType<T> type = beanManager.createEnhancedAnnotatedType((Class<T>) bean.getBeanClass());
        final Collection<EnhancedAnnotatedField<?, ? super T>> enhancedFields = type.getEnhancedFields();
        final EnhancedAnnotatedField<Object, T> eaf = (EnhancedAnnotatedField<Object, T>) enhancedFields.stream()
                .filter(it -> field.equals(it.getJavaMember()))
                .findFirst()
                .orElseThrow(() -> new TestEEfiException("Failed to get enhanced annotated field for " + field));
        return InjectionPointFactory.instance()
                .createFieldInjectionPoint(eaf, bean, bean.getBeanClass(), beanManager);
    }
}
