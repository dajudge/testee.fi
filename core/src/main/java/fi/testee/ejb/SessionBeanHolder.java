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
package fi.testee.ejb;

import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class SessionBeanHolder<T> implements ResourceReferenceFactory<T> {

    private final Set<SessionBeanLifecycleListener> listeners = new HashSet<>();

    public void addLifecycleListener(final SessionBeanLifecycleListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    protected void notifyListeners(final Consumer<SessionBeanLifecycleListener> c) {
        copy().forEach(c);
    }

    private Set<SessionBeanLifecycleListener> copy() {
        return new HashSet<>(listeners);
    }

    public abstract void forceDestroy();
}
