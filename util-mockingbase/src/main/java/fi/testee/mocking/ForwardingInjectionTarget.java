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
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.Set;

public class ForwardingInjectionTarget<T> implements InjectionTarget<T> {

    private final InjectionTarget<T> delegate;

    public ForwardingInjectionTarget(final InjectionTarget<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public T produce(final CreationalContext<T> ctx) {
        return delegate.produce(ctx);
    }

    @Override
    public void dispose(final T instance) {
        delegate.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return delegate.getInjectionPoints();
    }

    @Override
    public void inject(final T instance, final CreationalContext<T> ctx) {
        delegate.inject(instance, ctx);
    }

    @Override
    public void postConstruct(final T instance) {
        delegate.postConstruct(instance);
    }

    @Override
    public void preDestroy(final T instance) {
        delegate.preDestroy(instance);
    }
}
