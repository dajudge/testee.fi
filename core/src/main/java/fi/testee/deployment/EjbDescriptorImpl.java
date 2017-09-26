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

import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.Releaser;
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.InterceptorBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Remote;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

public class EjbDescriptorImpl<T> implements EjbDescriptor<T> {
    private static final Logger LOG = LoggerFactory.getLogger(EjbDescriptorImpl.class);

    private final Collection<BusinessInterfaceDescriptor<?>> localBusinessInterfaces;
    private final Collection<BusinessInterfaceDescriptor<?>> remoteBusinessInterfaces;
    private final Class<T> clazz;

    public EjbDescriptorImpl(final Class<T> clazz) {
        LOG.trace("Creating EJB descriptor for {}", clazz);
        this.clazz = clazz;
        localBusinessInterfaces = new HashSet<>();
        remoteBusinessInterfaces = new HashSet<>();
        localBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> clazz);
        stream(clazz.getInterfaces()).forEach(iface -> {
            if (iface.getAnnotation(Remote.class) != null) {
                remoteBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> iface);
            } else {
                localBusinessInterfaces.add((BusinessInterfaceDescriptor) () -> iface);
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
        return getBeanClass().getName();
    }

    @Override
    public Collection<Method> getRemoveMethods() {
        // TODO implement this
        try {
            return asList(getClass().getMethod("getRemoveMethods"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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


    @Override
    public String toString() {
        return "EjbDescriptorImpl for " + clazz;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o instanceof EjbDescriptorImpl) {
            EjbDescriptorImpl<?> that = (EjbDescriptorImpl<?>) o;
            return clazz.equals(that.clazz);
        }
        if (o instanceof EjbDescriptor) {
            return o.equals(this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

}
