package com.dajudge.testee.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Supplier;

import static java.lang.System.identityHashCode;
import static java.lang.reflect.Proxy.newProxyInstance;

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
    public static <T> T trace(final Object delegate, final Class<T> iface) {
        final String oid = delegate.getClass().getName() + "@" + identityHashCode(delegate);
        return (T) newProxyInstance(
                JdbcUtils.class.getClassLoader(),
                new Class<?>[]{iface},
                (proxy, method, args) -> {
                    try {
                        LOG.trace("CALL {} {} {}", oid, method, args == null ? "[]" : Arrays.asList(args));
                        if (method.getReturnType().isInterface()) {
                            return trace(method.invoke(delegate, args), method.getReturnType());
                        } else {
                            return method.invoke(delegate, args);
                        }
                    } catch (final InvocationTargetException e) {
                        throw e.getTargetException();
                    }
                }
        );
    }
}
