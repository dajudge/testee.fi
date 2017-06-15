package com.dajudge.testee.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

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

    public static <T> T lazy(final Supplier<T> delegateFactory, final Class<T> iface) {
        class Container<T> {
            T instance;
        }
        final Container<T> container = new Container<>();
        return (T) Proxy.newProxyInstance(
                delegateFactory.getClass().getClassLoader(),
                new Class[]{iface},
                (proxy, method, args) -> {
                    synchronized (container) {
                        if (container.instance == null) {
                            container.instance = delegateFactory.get();
                        }
                    }
                    try {
                        return method.invoke(container.instance, args);
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
    public static <T> T trace(final T delegate, final Class<T> iface) {
        return (T) Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                new Class[]{iface},
                (proxy, method, args) -> {
                    try {
                        if (method.getDeclaringClass() != Object.class) {
                            LOG.debug("CALL: {} {}", method, args == null ? "[]" : asList(args));
                        }
                        return method.invoke(delegate, args);
                    } catch (final InvocationTargetException e) {
                        throw e.getCause();
                    }
                }
        );
    }
}
