package com.dajudge.testee.mockito;

import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bean.ManagedBean;
import org.jboss.weld.bean.builtin.AbstractBuiltInBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class MockManager {
    private static final Logger LOG = LoggerFactory.getLogger(MockManager.class);

    private Collection<Object> mocks = new ArrayList<>();

    public MockManager(final Collection<Object> mocks) {
        this.mocks = Collections.unmodifiableCollection(mocks);
    }

    public boolean isMock(final Object needle) {
        for (final Object mock : mocks) {
            if (needle == mock) {
                return true;
            }
        }
        return false;
    }

    public <T> T findMockFor(final Class<T> beanClass) {
        for (final Object mock : mocks) {
            if (beanClass.isAssignableFrom(mock.getClass())) {
                return (T) mock;
            }
        }
        return null;
    }

    public <T> void wrapProducerFor(final Bean<T> bean) {
        if (bean instanceof AbstractClassBean) {
            ((ManagedBean) bean).setProducer(wrap(bean, ((ManagedBean) bean).getProducer()));
        } else if (bean instanceof AbstractBuiltInBean) {
            // Not replacing those
        } else {
            throw new UnsupportedOperationException("Unknown bean type encountered: " + bean);
        }
    }

    private <T> InjectionTarget wrap(Bean<T> bean, InjectionTarget<T> delegate) {
        return new InjectionTarget<T>() {
            @Override
            public void inject(final T instance, final CreationalContext<T> ctx) {
                if (!isMock(instance)) {
                    delegate.inject(instance, ctx);
                }
            }

            @Override
            public void postConstruct(final T instance) {
                delegate.postConstruct(instance);
            }

            @Override
            public void preDestroy(final T instance) {
                delegate.preDestroy(instance);
            }

            @Override
            public T produce(final CreationalContext<T> ctx) {
                final T mock = findMockFor((Class<T>) bean.getBeanClass());
                if (mock != null) {
                    LOG.debug("Injecting mock for {}", bean);
                    return mock;
                }
                return delegate.produce(ctx);
            }

            @Override
            public void dispose(final T instance) {
                delegate.dispose(instance);
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return delegate.getInjectionPoints();
            }
        };
    }
}
