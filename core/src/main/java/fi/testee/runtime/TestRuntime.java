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
import fi.testee.services.TransactionServicesImpl;
import fi.testee.spi.Releaser;
import fi.testee.spi.RuntimeLifecycleListener;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.bootstrap.api.helpers.SimpleServiceRegistry;
import org.jboss.weld.transaction.spi.TransactionServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fi.testee.deployment.DeploymentImpl.UNMODIFIED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.jboss.weld.bootstrap.api.Environments.SE;

/**
 * The holder of the static test runtime context. The class is thread safe.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class TestRuntime {
    private static final Logger LOG = LoggerFactory.getLogger(TestRuntime.class);
    private static TestRuntime instance;

    private final BeanArchiveDiscovery beanArchiveDiscovery = new BeanArchiveDiscovery();
    private final DependencyInjectionRealm realm;
    private final Releaser releaser = new Releaser();

    /**
     * Access to the singleton plugin registry.
     *
     * @return the plugin registry.
     */
    public static synchronized TestRuntime instance() {
        if (instance == null) {
            instance = new TestRuntime();
        }
        return instance;
    }

    private TestRuntime() {
        final ServiceRegistry serviceRegistry = new SimpleServiceRegistry();
        serviceRegistry.add(TransactionServices.class, new TransactionServicesImpl());
        final BeanDeployment beanDeployment = new BeanDeployment(beanArchiveDiscovery, BeanArchive::isFrameworkRelevant);
        realm = new DependencyInjectionRealm().init(
                serviceRegistry,
                SE,
                emptySet(),
                UNMODIFIED,
                asList(beanDeployment)
        );
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                releaser.release();
                realm.shutdown();
            }
        });
        LOG.trace("Notifying runtime lifecycle listeners about start");
        realm.getInstancesOf(RuntimeLifecycleListener.class, releaser).forEach(RuntimeLifecycleListener::onRuntimeStarted);
    }

    public BeanArchiveDiscovery getBeanArchiveDiscorvery() {
        return beanArchiveDiscovery;
    }
}
