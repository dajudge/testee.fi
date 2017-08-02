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

import fi.testee.spi.DependencyInjection;

import java.util.Set;
import java.util.function.Supplier;

class DeferredDependencyInjection implements DependencyInjection {
    private final Supplier<DependencyInjection> di;

    DeferredDependencyInjection(final Supplier<DependencyInjection> di) {
        this.di = di;
    }

    @Override
    public <T> Set<T> getInstancesOf(final Class<T> clazz) {
        return di.get().getInstancesOf(clazz);
    }

    @Override
    public <T> T getInstanceOf(final Class<T> clazz) {
        return di.get().getInstanceOf(clazz);
    }

    @Override
    public void inject(final Object o) {
        di.get().inject(o);
    }
}
