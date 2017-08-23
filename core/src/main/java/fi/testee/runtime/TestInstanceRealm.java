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

import fi.testee.deployment.BeanArchive;
import fi.testee.deployment.BeanArchiveDiscovery;
import fi.testee.deployment.BeanDeployment;
import fi.testee.services.ResourceInjectionServicesImpl;
import fi.testee.services.TransactionServicesImpl;
import fi.testee.spi.BeansXmlModifier;
import fi.testee.spi.CdiExtensionFactory;
import fi.testee.spi.DependencyInjection;
import fi.testee.spi.DynamicArchiveContributor;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.Releaser;
import fi.testee.spi.ResourceProvider;
import fi.testee.spi.SessionBeanAlternatives;
import fi.testee.spi.scope.TestInstanceScope;
import fi.testee.spi.scope.TestSetupScope;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.injection.spi.ResourceReferenceFactory;
import org.jboss.weld.transaction.spi.TransactionServices;

import javax.enterprise.inject.spi.Extension;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static fi.testee.deployment.DeploymentImpl.UNMODIFIED;
import static java.util.Arrays.asList;
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
        final BeanDeployment beanDeployment = new BeanDeployment(beanArchiveDiscovery, BeanArchive::isFrameworkRelevant);
        super.init(
                serviceRegistry(resourceProviders(testInstance, method, setupResourceProviders)),
                Environments.SE,
                emptySet(),
                UNMODIFIED,
                asList(beanDeployment)
        );
        this.instanceId = instanceId;
        this.testInstance = testInstance;

        final Set<ResourceProvider> resourceProviders = getInstancesOf(ResourceProvider.class, releaser);
        final BeansXmlModifier beansXmlModifier = beansXmlModifiers();
        final Collection<Metadata<Extension>> extensions = instanceExtensions(method);
        context = getInstanceOf(TransactionalContext.class, releaser);
        final Annotation[] scopes = {TestSetupScope.INSTANCE, TestInstanceScope.INSTANCE};
        final Collection<DynamicArchiveContributor> archiveContributors = getInstancesOf(
                DynamicArchiveContributor.class,
                releaser
        );
        context.initialize(
                extensions,
                beansXmlModifier,
                resourceProviders,
                it -> true,
                sessionBeanAlternatives(releaser),
                archiveContributors,
                scopes
        );
        context.getDependencyInjection().inject(testInstance, releaser);
        context.getDependencyInjection().postConstruct(testInstance);
        return this;
    }

    private SessionBeanAlternatives sessionBeanAlternatives(final Releaser releaser) {
        final Set<SessionBeanAlternatives> all = getInstancesOf(SessionBeanAlternatives.class, releaser);
        return type -> {
            // TODO handle ambiguous alternatives
            for (final SessionBeanAlternatives it : all) {
                final ResourceReferenceFactory<Object> alternative = it.alternativeFor(type);
                if (alternative != null) {
                    return alternative;
                }
            }
            return null;
        };
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

    private Collection<Metadata<Extension>> instanceExtensions(final Method method) {
        final Set<CdiExtensionFactory> factories = getInstancesOf(CdiExtensionFactory.class, releaser);
        return factories.stream()
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
        services.add(TransactionServices.class, new TransactionServicesImpl());
        return services;
    }

    @Override
    public <T> T create(final Class<T> clazz, final ReleaseCallbackHandler releaser) {
        return context.getDependencyInjection().getInstanceOf(clazz, releaser);
    }

    @Override
    public void shutdown() {
        context.flushEntityManagers();
        context.getDependencyInjection().preDestroy(testInstance);
        releaser.release();
        super.shutdown();
    }

    @Override
    public String getId() {
        return instanceId;
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
