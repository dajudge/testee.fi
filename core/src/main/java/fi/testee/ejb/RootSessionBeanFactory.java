package fi.testee.ejb;

import fi.testee.exceptions.TestEEfiException;
import fi.testee.spi.SessionBeanFactory;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import java.util.function.Consumer;

public class RootSessionBeanFactory<T> implements SessionBeanFactory<T> {
    private final Consumer<? super T> injection;
    private final EjbDescriptor<T> descriptor;

    public RootSessionBeanFactory(
            final Consumer<? super T> injection,
            final EjbDescriptor<T> descriptor
    ) {
        this.injection = injection;
        this.descriptor = descriptor;
    }

    @Override
    public EjbDescriptor<T> getDescriptor() {
        return descriptor;
    }

    @Override
    public ResourceReferenceFactory<T> getResourceReferenceFactory() {
        return new SingletonBeanContainer<>(descriptor.getBeanClass(), () -> {
            try {
                final T t = descriptor.getBeanClass().newInstance();
                injection.accept(t);
                return t;
            } catch (final InstantiationException | IllegalAccessException e) {
                throw new TestEEfiException("Failed to instantiate session bean", e);
            }
        });
    }
}
