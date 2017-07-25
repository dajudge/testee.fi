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
package fi.testee.deployment;

import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static java.util.Arrays.stream;

class EjbDescriptorImpl<T> implements EjbDescriptor<T> {
    private final Collection<BusinessInterfaceDescriptor<?>> localBusinessInterfaces;
    private final Collection<BusinessInterfaceDescriptor<?>> remoteBusinessInterfaces;
    private final Class<T> clazz;

    public EjbDescriptorImpl(final Class<T> clazz) {
        this.clazz = clazz;
        localBusinessInterfaces = new HashSet<>();
        remoteBusinessInterfaces = new HashSet<>();
        localBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> clazz);
        stream(clazz.getInterfaces()).forEach(iface -> {
            if (iface.getAnnotation(Local.class) != null) {
                localBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> iface);
            }
            if (iface.getAnnotation(Remove.class) != null) {
                remoteBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> iface);
            }
        });
    }

    @Override
    public Class<T> getBeanClass() {
        return clazz;
    }

    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getLocalBusinessInterfaces() {
        return localBusinessInterfaces;
    }

    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getRemoteBusinessInterfaces() {
        return remoteBusinessInterfaces;
    }

    @Override
    public String getEjbName() {
        // TODO do something sane here?
        return getBeanClass().getSimpleName();
    }

    @Override
    public Collection<Method> getRemoveMethods() {
        // TODO implement this
        return Collections.emptySet();
    }

    @Override
    public boolean isStateless() {
        return getBeanClass().getAnnotation(Stateless.class) != null;
    }

    @Override
    public boolean isSingleton() {
        return getBeanClass().getAnnotation(Singleton.class) != null;
    }

    @Override
    public boolean isStateful() {
        return getBeanClass().getAnnotation(Stateful.class) != null;
    }

    @Override
    public boolean isMessageDriven() {
        return false;
    }

    @Override
    public boolean isPassivationCapable() {
        return false;
    }
}
