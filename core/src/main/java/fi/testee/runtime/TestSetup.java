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
import fi.testee.jdbc.ConnectionFactoryManager;
import fi.testee.jdbc.TestDataSource;
import fi.testee.services.ResourceInjectionServicesImpl;
import fi.testee.services.TransactionServicesImpl;
import fi.testee.spi.AnnotationScanner;
import fi.testee.spi.ConnectionFactory;
import fi.testee.spi.DataSourceMigrator;
import fi.testee.spi.DependencyInjection;
import fi.testee.spi.ReleaseCallbackHandler;
import fi.testee.spi.Releaser;
import fi.testee.spi.ResourceProvider;
import fi.testee.spi.TestDataSetupAccessorFactory;
import fi.testee.spi.scope.TestSetupScope;
import org.jboss.weld.bootstrap.api.Environments;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static fi.testee.deployment.DeploymentImpl.UNMODIFIED;
import static fi.testee.runtime.ManualResourceProviderBuilder.manualResourceProvider;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

/**
 * Setup for a test. It contains the state shared by all instances of the test setup.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestSetup extends DependencyInjectionRealm {
    private static final Logger LOG = LoggerFactory.getLogger(TestSetup.class);

    private final Map<Class<? extends ConnectionFactory>, ConnectionFactory> connectionFactories = new HashMap<>();
    private final Class<?> setupClass;
    private final TestRuntime runtime;
    private final Releaser setupReleaser = new Releaser();

    public interface TestInstance {
        <T> T create(Class<T> clazz, ReleaseCallbackHandler releaser);

        void shutdown();

        String getId();
    }

    public TestSetup(
            final Class<?> setupClass,
            final TestRuntime runtime
    ) {
        this.setupClass = setupClass;
        this.runtime = runtime;
    }

    public TestSetup init() {
        final SimpleServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        final TransactionServicesImpl transactionServices = new TransactionServicesImpl();
        final ResourceProvider setupResources = createSetupResources(transactionServices, false);
        serviceRegistry.add(ResourceInjectionServices.class, new ResourceInjectionServicesImpl(asList(setupResources)));
        serviceRegistry.add(TransactionServicesImpl.class, transactionServices);
        final BeanArchiveDiscovery beanArchiveDiscovery = runtime.getBeanArchiveDiscorvery();
        final BeanDeployment beanDeployment = new BeanDeployment(beanArchiveDiscovery, BeanArchive::isFrameworkRelevant);
        super.init(
                serviceRegistry,
                Environments.SE,
                emptySet(),
                UNMODIFIED,
                asList(beanDeployment)
        );
        setupTestData(setupResources);
        return this;
    }

    private void setupTestData(final ResourceProvider setupResources) {
        try {
            final Releaser testDataReleaser = new Releaser();
            final TransactionalContext context = getInstanceOf(TransactionalContext.class, testDataReleaser);
            final Annotation[] scopes = {TestSetupScope.INSTANCE};
            context.initialize(
                    emptySet(),
                    UNMODIFIED,
                    asList(setupResources),
                    BeanArchive::isFrameworkRelevant,
                    type -> null,
                    emptySet(),
                    scopes
            );
            try {
                final Set<DataSourceMigrator> migrators = getInstancesOf(DataSourceMigrator.class, testDataReleaser);
                final ServiceRegistry serviceRegistry = context.getDependencyInjection().getServiceRegistry();
                DatabaseMigration.migrateDataSources(setupClass, migrators, serviceRegistry);
                getInstanceOf(TestDataSetup.class, testDataReleaser).setupTestData(setupClass, serviceRegistry);
                context.flushEntityManagers();
            } finally {
                testDataReleaser.release();
            }
        } catch (final Exception e) {
            shutdown();
            throw e;
        }
    }

    private ResourceProvider createSetupResources(
            final TransactionServices transactionServices,
            final boolean rollbackTransactions
    ) {
        return manualResourceProvider()
                .put("testeefi/setup/class", setupClass)
                .put("testeefi/setup/beanArchiveDiscovery", runtime.getBeanArchiveDiscorvery())
                .put("testeefi/setup/connectionFactoryManager", (ConnectionFactoryManager) this::connectionFactoryManager)
                .put("testeefi/setup/dependencyInjection", testSetupDependencyInjection())
                .put("testeefi/setup/annotationScanner", (AnnotationScanner) runtime.getBeanArchiveDiscorvery()::getClassesWith)
                .put("testeefi/setup/rollbackTransactions", rollbackTransactions)
                .put("testeefi/setup/transactionServices", transactionServices)
                .build();
    }

    private DependencyInjection testSetupDependencyInjection() {
        return new DeferredDependencyInjection(() -> this);
    }

    private synchronized ConnectionFactory connectionFactoryManager(TestDataSource testDataSource) {
        if (!connectionFactories.containsKey(testDataSource.factory())) {
            connectionFactories.put(testDataSource.factory(), getInstanceOf(testDataSource.factory(), setupReleaser));
        }
        return connectionFactories.get(testDataSource.factory());
    }


    public TestInstance prepareTestInstance(final String id, final Object testInstance, final Method method) {
        LOG.debug("Instantiating test run '{}' for class {}", id, testInstance.getClass().getName());
        final TestInstanceRealm context = new TestInstanceRealm();
        return context.init(
                runtime.getBeanArchiveDiscorvery(),
                id,
                testInstance,
                method,
                asList(createSetupResources(getServiceRegistry().get(TransactionServicesImpl.class), true))
        );
    }

    @Override
    public void shutdown() {
        setupReleaser.release();
        super.shutdown();
    }
}
