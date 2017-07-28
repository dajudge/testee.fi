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
import org.jboss.weld.context.CreationalContextImpl;
import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.ejb.spi.InterceptorBindings;

import javax.ejb.Local;
import javax.ejb.Remove;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.stream;

public class EjbDescriptorImpl<T> implements EjbDescriptor<T> {
    private final Collection<BusinessInterfaceDescriptor<?>> localBusinessInterfaces;
    private final Collection<BusinessInterfaceDescriptor<?>> remoteBusinessInterfaces;
    private final Class<T> clazz;
    private InterceptorBindings interceptorBindings;

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
        return getBeanClass().getName();
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

    public void setInterceptorBindings(final InterceptorBindings interceptorBindings) {
        this.interceptorBindings = interceptorBindings;
    }


    private interface Proceed {
        Object run() throws Throwable;
    }

    public InterceptorChain getInterceptorChain() {
        return new InterceptorChain() {
            @Override
            public <T> Object invoke(
                    final Object target,
                    final Method method,
                    final Object[] args,
                    final ChainEnd<T> next
            ) throws Throwable {
                if (interceptorBindings == null) {
                    return next.invoke();
                }
                return processInterceptorChain(
                        new ArrayList<>(interceptorBindings.getAllInterceptors()),
                        target,
                        method,
                        args,
                        next
                );
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> Object processInterceptorChain(
            final List<Interceptor<?>> chain,
            final Object target,
            final Method method,
            final Object[] args,
            final InterceptorChain.ChainEnd<T> next
    ) throws Throwable {
        if (chain.isEmpty()) {
            return next.invoke();
        }
        final Interceptor<Object> it = (Interceptor<Object>) chain.remove(0);
        return intercept(
                it,
                it.create(new CreationalContextImpl<>(null)),
                target,
                method,
                args,
                () -> processInterceptorChain(chain, target, method, args, next)
        );
    }

    private <T> Object intercept(
            final Interceptor<T> it,
            final T instance,
            final Object target,
            final Method method,
            final Object[] args,
            final Proceed proceed
    ) throws Exception {
        return it.intercept(
                InterceptionType.AROUND_INVOKE,
                instance,
                context(target, method, args, proceed)
        );
    }

    private InvocationContext context(
            final Object target,
            final Method method,
            final Object[] args,
            final Proceed proceed
    ) {
        return new InvocationContext() {
            @Override
            public Object getTarget() {
                return target;
            }

            @Override
            public Object getTimer() {
                return null;
            }

            @Override
            public Method getMethod() {
                return method;
            }

            @Override
            public Constructor<?> getConstructor() {
                return null;
            }

            @Override
            public Object[] getParameters() {
                return args;
            }

            @Override
            public void setParameters(final Object[] params) {
                throw new UnsupportedOperationException("Changing parameters is not supported, yet");
            }

            @Override
            public Map<String, Object> getContextData() {
                return new HashMap<>();
            }

            @Override
            public Object proceed() throws Exception {
                try {
                    return proceed.run();
                } catch (final Exception e) {
                    throw e;
                } catch (final Throwable throwable) {
                    throw new TestEEfiException("Failed to process interceptor chain", throwable);
                }
            }
        };
    }
}
