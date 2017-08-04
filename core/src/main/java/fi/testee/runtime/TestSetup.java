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

import fi.testee.ejb.EjbContainer;
import fi.testee.jdbc.ConnectionFactoryManager;
import fi.testee.jdbc.TestDataSource;
import fi.testee.services.ResourceInjectionServicesImpl;
import fi.testee.spi.AnnotationScanner;
import fi.testee.spi.BeanModifier;
import fi.testee.spi.BeanModifierFactory;
import fi.testee.spi.BeansXmlModifier;
import fi.testee.spi.CdiExtensionFactory;
import fi.testee.spi.ConnectionFactory;
import fi.testee.spi.DataSourceMigrator;
import fi.testee.spi.DependencyInjection;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.Releaser;
import fi.testee.spi.SessionBeanFactory;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.bootstrap.spi.BeansXml;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Extension;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static fi.testee.deployment.DeploymentImpl.UNMODIFIED;
import static fi.testee.runtime.TestDataSetup.setupTestData;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

/**
 * Setup for a test. It contains the state shared by all instances of the test setup.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(TestSetup.class);
    private final DependencyInjectionRealm realm;
    private final Map<Class<? extends ConnectionFactory>, ConnectionFactory> connectionFactories = new HashMap<>();
    private final Map<String, Object> setupResources;
    private Releaser setupReleaser = new Releaser();

    public interface TestContext {
        <T> T create(Class<T> clazz, ReleaseCallbackHandler releaser);

        void shutdown();

        String getId();
    }

    public TestSetup(
            final Class<?> setupClass,
            final TestRuntime runtime
    ) {
        final ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        setupResources = createSetupResources(setupClass, runtime);
        final SimpleResourceProvider resourceProvider = new SimpleResourceProvider(setupResources);
        serviceRegistry.add(ResourceInjectionServices.class, new ResourceInjectionServicesImpl(asList(resourceProvider)));
        realm = new DependencyInjectionRealm(
                serviceRegistry,
                runtime.getBeanArchiveDiscorvery(),
                Environments.SE,
                emptySet(),
                UNMODIFIED);
        final Releaser releaser = new Releaser();
        final TransactionalContext txContext = realm.getInstanceOf(TransactionalContext.class, releaser);
        try {
            txContext.initialize(EjbContainer.IDENTITY_SESSION_BEAN_MODIFIER, emptyMap(), emptySet(), UNMODIFIED);
            txContext.run((clazz, testDataSetupRealm) -> {
                final Set<DataSourceMigrator> migrators = testDataSetupRealm.getInstancesOf(DataSourceMigrator.class, releaser);
                DatabaseMigration.migrateDataSources(clazz, migrators, testDataSetupRealm.getServiceRegistry());
                setupTestData(clazz, testDataSetupRealm.getServiceRegistry());
                return null;
            });
            txContext.commit();
            releaser.release();
        } catch (final RuntimeException e) {
            txContext.rollback();
            releaser.release();
            shutdown();
            throw e;
        }
    }

    private Map<String, Object> createSetupResources(final Class<?> setupClass, final TestRuntime runtime) {
        final Map<String, Object> ret = new HashMap<>();
        ret.put("testeefi/setup/class", setupClass);
        ret.put("testeefi/setup/beanArchiveDiscovery", runtime.getBeanArchiveDiscorvery());
        ret.put("testeefi/setup/connectionFactoryManager", (ConnectionFactoryManager) this::connectionFactoryManager);
        ret.put("testeefi/setup/dependencyInjection", testSetupDependencyInjection());
        ret.put("testeefi/setup/annotationScanner", (AnnotationScanner) runtime.getBeanArchiveDiscorvery()::getClassesWith);
        return ret;
    }

    private DependencyInjection testSetupDependencyInjection() {
        return new DeferredDependencyInjection(() -> realm);
    }

    private synchronized ConnectionFactory connectionFactoryManager(TestDataSource testDataSource) {
        if (!connectionFactories.containsKey(testDataSource.factory())) {
            connectionFactories.put(testDataSource.factory(), realm.getInstanceOf(testDataSource.factory(), setupReleaser));
        }
        return connectionFactories.get(testDataSource.factory());
    }


    public TestContext prepareTestInstance(final String id, final Object testInstance, final Method method) {
        LOG.debug("Instantiating test run '{}' for class {}", id, testInstance.getClass().getName());
        final Releaser instanceReleaser = new Releaser();
        final Set<BeanModifier> beanModifiers = beanModifiers(testInstance, instanceReleaser);
        final TransactionalContext txContext = realm.getInstanceOf(TransactionalContext.class, instanceReleaser);
        final Collection<Metadata<Extension>> extensions = instanceExtensions(method, instanceReleaser);
        final BeansXmlModifier beansXmlModifier = beansXmlModifiers(realm, instanceReleaser);
        txContext.initialize(new SessionBeanModifierImpl(beanModifiers), setupResources, extensions, beansXmlModifier);
        txContext.run((clazz, testInstanceRealm) -> {
            testInstanceRealm.getAllBeans().forEach(modifyCdiBeans(beanModifiers));
            testInstanceRealm.inject(testInstance, instanceReleaser);
            testInstanceRealm.postConstruct(testInstance);
            return null;
        });
        return new TestContext() {
            @Override
            public <T> T create(final Class<T> clazz, final ReleaseCallbackHandler releaser) {
                return txContext.run((testSetupClass, realm) -> realm.getInstanceOf(clazz, releaser));
            }

            @Override
            public void shutdown() {
                txContext.run((clazz, testInstanceRealm) -> {
                    testInstanceRealm.preDestroy(testInstance);
                    return null;
                });
                instanceReleaser.release();
                txContext.rollback();
            }

            @Override
            public String getId() {
                return id;
            }
        };
    }

    private BeansXmlModifier beansXmlModifiers(final DependencyInjectionRealm realm, final Releaser releaser) {
        final Set<BeansXmlModifier> modifiers = realm.getInstancesOf(BeansXmlModifier.class, releaser);
        return beansXml -> {
            BeansXml ret = beansXml;
            for (final BeansXmlModifier modifier : modifiers) {
                ret = modifier.apply(ret);
            }
            return ret;
        };
    }

    private Set<BeanModifier> beanModifiers(final Object testInstance, final Releaser releaser) {
        return realm.getInstancesOf(BeanModifierFactory.class, releaser).stream()
                .map(it -> it.createBeanModifier(testInstance))
                .collect(toSet());
    }

    private Collection<Metadata<Extension>> instanceExtensions(final Method method, final Releaser releaser) {
        return realm.getInstancesOf(CdiExtensionFactory.class, releaser).stream()
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

    private Consumer<Bean<?>> modifyCdiBeans(final Collection<BeanModifier> beanModifiers) {
        return bean -> beanModifiers.forEach(it -> it.modifyCdiBean(bean));
    }

    public void shutdown() {
        setupReleaser.release();
        realm.shutdown();
    }

    private static class SessionBeanModifierImpl implements EjbContainer.SessionBeanModifier {
        private final Set<BeanModifier> beanModifiers;

        SessionBeanModifierImpl(final Set<BeanModifier> beanModifiers) {
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

}
