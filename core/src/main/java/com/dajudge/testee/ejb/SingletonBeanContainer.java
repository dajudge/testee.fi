package com.dajudge.testee.ejb;

import com.dajudge.testee.exceptions.TesteeException;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;

import java.util.function.Consumer;

public class SingletonBeanContainer implements SessionBeanContainer {
    private final EjbDescriptor<?> descriptor;
    private final Consumer<Object> injection;
    private Object instance;

    public SingletonBeanContainer(
            final EjbDescriptor<?> descriptor,
            final Consumer<Object> injection
    ) {
        this.descriptor = descriptor;
        this.injection = injection;
    }

    @Override
    public ResourceReferenceFactory<Object> get() {
        return () -> new ResourceReference<Object>() {

            @Override
            public Object getInstance() {
                synchronized (SingletonBeanContainer.this) {
                    if (null == instance) {
                        try {
                            instance = descriptor.getBeanClass().newInstance();
                            injection.accept(instance);
                        } catch (IllegalAccessException | InstantiationException e) {
                            throw new TesteeException(
                                    "Failed to instantiate session bean: " + descriptor.getBeanClass(),
                                    e
                            );
                        }
                    }
                    return instance;
                }
            }

            @Override
            public void release() {

            }
        };
    }
}
