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
package fi.testee.rest;

import fi.testee.spi.DependencyInjection;
import fi.testee.spi.Releaser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.hk2.api.Factory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class DependencyInjectionFactory<T> implements Factory<T> {
    private final Class<T> component;
    private final DependencyInjection dependencyInjector;
    private final Set<Pair<Object, Releaser>> releasers = new HashSet<>();

    public DependencyInjectionFactory(final Class<T> component, final DependencyInjection dependencyInjector) {
        this.component = component;
        this.dependencyInjector = dependencyInjector;
    }

    @Override
    public T provide() {
        final Releaser releaser = new Releaser();
        final T instance = dependencyInjector.getInstanceOf(component, releaser);
        synchronized (releasers) {
            releasers.add(new ImmutablePair<>(instance, releaser));
        }
        return instance;
    }

    @Override
    public void dispose(final T instance) {
        synchronized (releasers) {
            final Iterator<Pair<Object, Releaser>> it = releasers.iterator();
            while (it.hasNext()) {
                final Pair<Object, Releaser> c = it.next();
                if (c.getLeft() == instance) {
                    c.getRight().release();
                    it.remove();
                    return;
                }
            }
        }
        throw new IllegalStateException("Cannot dispose unknown object: " + instance);
    }
}
