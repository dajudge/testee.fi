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
package fi.testee.runtime;

import fi.testee.deployment.BeanArchiveDiscovery;
import fi.testee.ejb.EjbContainer;
import fi.testee.services.ResourceInjectionServicesImpl;
import fi.testee.spi.BeanModifier;
import fi.testee.spi.BeanModifierFactory;
import fi.testee.spi.BeansXmlModifier;
import fi.testee.spi.CdiExtensionFactory;
import fi.testee.spi.DependencyInjection;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.Releaser;
import fi.testee.spi.ResourceProvider;
import fi.testee.spi.SessionBeanFactory;
import fi.testee.spi.scope.TestInstanceScope;
import fi.testee.spi.scope.TestSetupScope;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.injection.spi.ResourceInjectionServices;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static fi.testee.deployment.DeploymentImpl.UNMODIFIED;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public class TestInstanceRealm extends DependencyInjectionRealm implements TestSetup.TestInstance {
    private final Releaser releaser = new Releaser();
    private TransactionalContext context;
    private String instanceId;
    private Object testInstance;

    public TestInstanceRealm init(
            final BeanArchiveDiscovery beanArchiveDiscovery,
            final String instanceId,
            final Object testInstance,
            final Method method,
            final Collection<ResourceProvider> setupResourceProviders
    ) {
        super.init(
                serviceRegistry(resourceProviders(testInstance, method, setupResourceProviders)),
                beanArchiveDiscovery,
                Environments.SE,
                emptySet(),
                UNMODIFIED
        );
        this.instanceId = instanceId;
        this.testInstance = testInstance;

        final Set<ResourceProvider> resourceProviders = getInstancesOf(ResourceProvider.class, releaser);
        final Set<BeanModifier> beanModifiers = beanModifiers(testInstance);
        final SessionBeanModifierImpl beanModifier = new SessionBeanModifierImpl(beanModifiers);
        final BeansXmlModifier beansXmlModifier = beansXmlModifiers();
        final Collection<Metadata<Extension>> extensions = instanceExtensions(method);
        context = getInstanceOf(TransactionalContext.class, releaser);
        final Annotation[] scopes = {TestSetupScope.INSTANCE, TestInstanceScope.INSTANCE};
        context.initialize(beanModifier, extensions, beansXmlModifier, resourceProviders, scopes);
        context.run((clazz, testInstanceRealm) -> {
            testInstanceRealm.getAllBeans().forEach(modifyCdiBeans(beanModifiers));
            testInstanceRealm.inject(testInstance, releaser);
            testInstanceRealm.postConstruct(testInstance);
            return null;
        });
        return this;
    }

    private Collection<ResourceProvider> resourceProviders(
            final Object testInstance,
            final Method method,
            final Collection<ResourceProvider> setupResourceProviders
    ) {
        final Collection<ResourceProvider> ret = new HashSet<>(setupResourceProviders);
        ret.add(new ManualResourceProviderBuilder()
                .put("testeefi/instance/instance", testInstance)
                .put("testeefi/instance/method", method)
                .put("testeefi/instance/dependencyInjection", dependencyInjection())
                .build());
        return ret;
    }

    private DependencyInjection dependencyInjection() {
        return new DeferredDependencyInjection(() -> context.getDependencyInjection());
    }

    private Consumer<Bean<?>> modifyCdiBeans(final Collection<BeanModifier> beanModifiers) {
        return bean -> beanModifiers.forEach(it -> it.modifyCdiBean(bean));
    }

    private Collection<Metadata<Extension>> instanceExtensions(final Method method) {
        return getInstancesOf(CdiExtensionFactory.class, releaser).stream()
                .map(factory -> factory.create(method))
                .filter(Objects::nonNull)
                .map(this::testExtension)
                .collect(toSet());
    }

    private Metadata<Extension> testExtension(final Extension instance) {
        return new Metadata<Extension>() {
            @Override
            public Extension getValue() {
                return instance;
            }

            @Override
            public String getLocation() {
                return "TestEE.fi-extension:" + instance.getClass().getName();
            }
        };
    }

    private static ServiceRegistry serviceRegistry(final Collection<ResourceProvider> resourceProviders) {
        final SimpleServiceRegistry services = new SimpleServiceRegistry();
        services.add(ResourceInjectionServices.class, new ResourceInjectionServicesImpl(resourceProviders));
        return services;
    }

    private Set<BeanModifier> beanModifiers(final Object testInstance) {
        return getInstancesOf(BeanModifierFactory.class, releaser).stream()
                .map(it -> it.createBeanModifier(testInstance))
                .collect(toSet());
    }

    @Override
    public <T> T create(final Class<T> clazz, final ReleaseCallbackHandler releaser) {
        return null;
    }

    @Override
    public void shutdown() {
        context.flushEntityManagers();
        context.run((clazz, testInstanceRealm) -> {
            testInstanceRealm.preDestroy(testInstance);
            return null;
        });
        releaser.release();
        super.shutdown();
    }

    @Override
    public String getId() {
        return instanceId;
    }

    private static class SessionBeanModifierImpl implements EjbContainer.SessionBeanModifier {
        private final Set<BeanModifier> beanModifiers;

        private SessionBeanModifierImpl(final Set<BeanModifier> beanModifiers) {
            this.beanModifiers = beanModifiers;
        }

        @Override
        public <T> SessionBeanFactory<T> modify(SessionBeanFactory<T> factory) {
            for (final BeanModifier beanModifier : beanModifiers) {
                factory = beanModifier.modifySessionBean(factory);
            }
            return factory;
        }
    }

    private BeansXmlModifier beansXmlModifiers() {
        final Set<BeansXmlModifier> modifiers = getInstancesOf(BeansXmlModifier.class, releaser);
        return beansXml -> {
            BeansXml ret = beansXml;
            for (final BeansXmlModifier modifier : modifiers) {
                ret = modifier.apply(ret);
            }
            return ret;
        };
    }
}
