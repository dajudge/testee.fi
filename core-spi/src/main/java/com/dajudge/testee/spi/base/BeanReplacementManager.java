package com.dajudge.testee.spi.base;

import com.dajudge.testee.spi.SessionBeanFactory;
import org.jboss.weld.bean.AbstractClassBean;
import org.jboss.weld.bean.builtin.AbstractBuiltInBean;
import org.jboss.weld.ejb.spi.EjbDescriptor;
import org.jboss.weld.injection.spi.ResourceReference;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
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
import java.util.function.Supplier;

class BeanReplacementManager {
    private static final Logger LOG = LoggerFactory.getLogger(BeanReplacementManager.class);

    private Collection<Object> replacements = new ArrayList<>();

    BeanReplacementManager(final Collection<Object> replacements) {
        this.replacements = Collections.unmodifiableCollection(replacements);
    }

    boolean isReplaced(final Object needle) {
        for (final Object replacement : replacements) {
            if (needle == replacement) {
                return true;
            }
        }
        return false;
    }

    <T> T findReplacementFor(final Class<T> beanClass) {
        for (final Object replacement : replacements) {
            if (beanClass.isAssignableFrom(replacement.getClass())) {
                return (T) replacement;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    <T> void instrumentCdiBean(final Bean<T> bean) {
        if (bean instanceof AbstractClassBean) {
            ((AbstractClassBean) bean).setProducer(wrap(bean, ((AbstractClassBean) bean).getProducer()));
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
                if (!isReplaced(instance)) {
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
            @SuppressWarnings("unchecked")
            public T produce(final CreationalContext<T> ctx) {
                Class<T> clazz = (Class<T>) bean.getBeanClass();
                Supplier<T> producer = () -> delegate.produce(ctx);
                final T replacement = findReplacementFor(clazz);
                if (replacement != null) {
                    LOG.debug("Injecting replacement for {}", clazz);
                    return replacement;
                }
                return producer.get();
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

    <T> SessionBeanFactory<T> wrapSessionBean(final SessionBeanFactory<T> sessionBean) {
        return new SessionBeanFactory<T>() {
            @Override
            public EjbDescriptor<T> getDescriptor() {
                return sessionBean.getDescriptor();
            }

            @Override
            public ResourceReferenceFactory<T> getResourceReferenceFactory() {
                return () -> {
                    final T replacement = findReplacementFor(sessionBean.getDescriptor().getBeanClass());
                    if (replacement != null) {
                        return new ResourceReference<T>() {
                            @Override
                            public T getInstance() {
                                return replacement;
                            }

                            @Override
                            public void release() {
                                // Replacements don't need to be released
                            }
                        };
                    }
                    return sessionBean.getResourceReferenceFactory().createResource();
                };
            }
        };
    }
}
