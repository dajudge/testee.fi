package fi.testee.ejb;

import fi.testee.exceptions.TestEEfiException;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import javax.inject.Provider;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

public class SingletonBeanContainer<T> implements ResourceReferenceFactory<T> {
    private final Provider<T> factory;
    private final T proxyInstance;
    private T instance;

    public SingletonBeanContainer(
            final Class<T> clazz,
            final Provider<T> factory
    ) {
        this.factory = factory;
        proxyInstance = createProxy(clazz, this::instance);
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxy(
            final Class<T> clazz,
            final Supplier<T> producer
    ) {
        try {
            final ProxyFactory proxyFactory = new ProxyFactory();
            proxyFactory.setSuperclass(clazz);
            proxyFactory.setFilter(m -> m.getDeclaringClass() != Object.class);
            final Class<T> proxyClass = proxyFactory.createClass();
            final Object instance = proxyClass.newInstance();
            ((ProxyObject) instance).setHandler(methodHandler(producer));
            return (T) instance;
        } catch (final IllegalAccessException | InstantiationException e) {
            throw new TestEEfiException("Failed to create proxy instance of" + clazz, e);
        }
    }

    private static <T> MethodHandler methodHandler(final Supplier<T> producer) {
        return (self, thisMethod, proceed, args) -> {
            try {
                return thisMethod.invoke(producer.get(), args);
            } catch (final InvocationTargetException e) {
                throw e.getTargetException();
            }
        };
    }

    @Override
    public ResourceReference<T> createResource() {
        return new ResourceReference<T>() {
            @Override
            public T getInstance() {
                return proxyInstance;
            }

            @Override
            public void release() {
                // TODO implement this
            }
        };
    }

    private synchronized T instance() {
        if (null == instance) {
            instance = factory.get();
        }
        return instance;
    }
}
