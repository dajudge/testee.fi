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
package fi.testee.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Supplier;

import static fi.testee.utils.ReflectionUtils.objectId;
import static java.lang.reflect.Proxy.newProxyInstance;
import static java.util.Arrays.asList;

/**
 * Tools useful for development.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public final class ProxyUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyUtils.class);

    private ProxyUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T lazy(final Supplier<T> delegateFactory, final Class<T> iface) {
        final MutableContainer<T> container = new MutableContainer<>();
        return (T) Proxy.newProxyInstance(
                delegateFactory.getClass().getClassLoader(),
                new Class[]{iface},
                (proxy, method, args) -> {
                    synchronized (container) {
                        if (container.getObject() == null) {
                            container.setObject(delegateFactory.get());
                        }
                    }
                    try {
                        return method.invoke(container.getObject(), args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
        );
    }

    /**
     * Wraps a proxy around something that implements some interface and logs all the calls (except those to methods
     * declared by {@link Object}).
     *
     * @param delegate the actual object to perform the invocations on.
     * @param iface    the interface to wrap.
     * @param <T>      the interface type to wrap.
     * @return the logging proxy.
     */
    @SuppressWarnings("unchecked")
    public static <T> T trace(final Object delegate, final Class<T> iface) {
        final String oid = objectId(delegate);
        return (T) newProxyInstance(
                JdbcUtils.class.getClassLoader(),
                new Class<?>[]{iface},
                (proxy, method, args) -> {
                    final String callId = UUID.randomUUID().toString();
                    try {
                        LOG.trace("ENTER {} {} {} {}", callId, oid, method, args == null ? "[]" : asList(args));
                        final Object ret = method.invoke(delegate, args);
                        LOG.trace("RETURN {} {} {} {} {}", callId, oid, method, args == null ? "[]" : asList(args), ret);
                        return ret;
                    } catch (final InvocationTargetException e) {
                        LOG.trace("THROW {} {} {} {} {}", callId, oid, method, args == null ? "[]" : asList(args), e);
                        throw e.getTargetException();
                    }
                }
        );
    }

}
