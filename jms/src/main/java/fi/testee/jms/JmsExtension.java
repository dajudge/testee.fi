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
package fi.testee.jms;

import fi.testee.exceptions.TestEEfiException;

import javax.ejb.MessageDriven;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.jms.MessageListener;

public class JmsExtension implements Extension {
    private final MessageDrivenBeanRegistry registry;

    public JmsExtension(final MessageDrivenBeanRegistry registry) {
        this.registry = registry;
    }

    public <X> void beans(
            final @Observes ProcessAnnotatedType<X> processBean
    ) {
        final MessageDriven annotation = processBean.getAnnotatedType().getAnnotation(MessageDriven.class);
        if (annotation != null) {
            final Class<X> javaClass = processBean.getAnnotatedType().getJavaClass();
            if (!MessageListener.class.isAssignableFrom(javaClass)) {
                throw new TestEEfiException("The @MessageDriven bean " + javaClass.getName()
                        + " does not implement MessageListener"
                );
            }
            registry.register(annotation, (Class<? extends MessageListener>) javaClass);
        }
    }
}
