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

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.enterprise.inject.spi.ProducerFactory;
import java.util.Collections;
import java.util.Set;

public class MockProducerFactory<T> implements ProducerFactory<T> {
    private final Object mock;

    public MockProducerFactory(final Object mock) {
        this.mock = mock;
    }

    @Override
    public <T1> Producer<T1> createProducer(final Bean<T1> bean) {
        return new Producer<T1>() {

            @Override
            public T1 produce(final CreationalContext<T1> ctx) {
                return (T1) mock;
            }

            @Override
            public void dispose(final T1 instance) {
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return Collections.emptySet();
            }
        };
    }
}
