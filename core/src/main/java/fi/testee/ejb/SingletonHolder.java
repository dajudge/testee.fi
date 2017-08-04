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

import fi.testee.deployment.InterceptorChain;
import fi.testee.exceptions.TestEEfiException;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.weld.injection.spi.ResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.InterceptionType;
import javax.inject.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static javax.enterprise.inject.spi.InterceptionType.AROUND_INVOKE;
import static javax.enterprise.inject.spi.InterceptionType.POST_CONSTRUCT;
import static javax.enterprise.inject.spi.InterceptionType.PRE_DESTROY;

public class SingletonHolder<T> extends SessionBeanHolder<T> {
    private static final Logger LOG = LoggerFactory.getLogger(SingletonHolder.class);
    private final Class<T> beanClass;
    private final Provider<Pair<T, Collection<ResourceReference<?>>>> factory;
    private final T proxyInstance;
    private T instance;
    private InterceptorChain chain;
    private int referenceCount = 0;
    private Collection<ResourceReference<?>> referenced;
    private boolean destroying;

    public SingletonHolder(
            final Class<T> beanClass,
            final Provider<Pair<T, Collection<ResourceReference<?>>>> factory,
            final InterceptorChain chain
    ) {
        this.beanClass = beanClass;
        this.factory = factory;
        proxyInstance = createProxy(beanClass, chain);
    }

    @SuppressWarnings("unchecked")
    private T createProxy(
            final Class<T> clazz,
            final InterceptorChain chain
    ) {
        this.chain = chain;
        try {
            final ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setSuperclass(clazz);
            proxyFactory.setFilter(m -> m.getDeclaringClass() != Object.class);
            final Class<T> proxyClass = proxyFactory.createClass();
            final Object instance = proxyClass.newInstance();
            ((ProxyObject) instance).setHandler(methodHandler());
            return (T) instance;
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new TestEEfiException("Failed to create proxy instance of " + clazz, e);
        }
    }

    private MethodHandler methodHandler() {
        return (self, thisMethod, proceed, args) -> invokeIntercepted(args, instance(), thisMethod, AROUND_INVOKE);
    }

    private Object invokeIntercepted(
            final Object[] args,
            final T target,
            final Method method,
            final InterceptionType interceptionType
    ) throws Throwable {
        return chain.invoke(target, method, args,
                () -> {
                    try {
                        return method.invoke(target, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }, interceptionType);
    }

    @Override
    public ResourceReference<T> createResource() {
        synchronized (SingletonHolder.this) {
            referenceCount++;
            LOG.trace("Creating resource reference to {}, count is now {}", beanClass, referenceCount);
        }

        return new ResourceReference<T>() {
            @Override
            public T getInstance() {
                return proxyInstance;
            }

            @Override
            public void release() {
                releaseInstance().run();
            }

            private Runnable releaseInstance() {
                synchronized (SingletonHolder.this) {
                    referenceCount -= 1;
                    LOG.trace("Releasing resource reference to {}, count is now {}", beanClass, referenceCount);
                    if (referenceCount == 0) {
                        LOG.trace("Last reference to {} released, destroying", beanClass);
                        if (instance != null && !destroying) {
                            return destroy();
                        } else {
                            LOG.trace("Last reference to {} released but not instantiated anyway", beanClass);
                        }
                        instance = null;
                        referenced = null;
                    }
                }
                return () -> {
                };
            }
        };
    }

    private Runnable destroy() {
        final T instanceToRelease = instance;
        return () -> {
            destroying = true;
            invoke(instanceToRelease, PreDestroy.class, PRE_DESTROY);
            notifyListeners(it -> it.destroyed(SingletonHolder.this));
            referenced.forEach(ResourceReference::release);
        };
    }

    private synchronized T instance() {
        if (null == instance) {
            final Pair<T, Collection<ResourceReference<?>>> pair = factory.get();
            instance = pair.getLeft();
            referenced = pair.getRight();
            invoke(instance, PostConstruct.class, POST_CONSTRUCT);
            notifyListeners(it -> it.constructed(this));
        }
        return instance;
    }

    private void invoke(final T t, final Class<? extends Annotation> annotation, final InterceptionType type) {
        Class<?> c = t.getClass();
        while (c != null && c != Object.class) {
            invoke(t, c, annotation, type);
            c = c.getSuperclass();
        }
    }

    private void invoke(
            final T t,
            final Class<?> c,
            final Class<? extends Annotation> annotation,
            final InterceptionType interceptionType
    ) {
        final Set<Method> candidates = stream(c.getDeclaredMethods())
                .filter(it -> it.getAnnotation(annotation) != null)
                .collect(toSet());
        if (candidates.isEmpty()) {
            return;
        }
        if (candidates.size() > 1) {
            throw new TestEEfiException("Only one @" + annotation.getSimpleName() + " method is allowed per class");
        }
        // TODO check for correct modifiers etc.
        final Method method = candidates.iterator().next();
        method.setAccessible(true);
        try {
            invokeIntercepted(new Object[]{}, t, method, interceptionType);
        } catch (final Throwable e) {
            throw new TestEEfiException("Failed to invoke @" + annotation.getSimpleName() + " method " + method, e);
        }
    }

    @Override
    public void forceDestroy() {
        destroy().run();
    }
}
