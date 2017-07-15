package com.dajudge.testee.eclipselink;

import com.dajudge.testee.spi.RuntimeLifecycleListener;
import org.eclipse.persistence.internal.jpa.deployment.PersistenceUnitProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers custom {@link org.eclipse.persistence.jpa.ArchiveFactory} on TestEE runtime startup.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EclipseLinkRuntimeLifecycleListener implements RuntimeLifecycleListener {
    private static final Logger LOG = LoggerFactory.getLogger(EclipseLinkRuntimeLifecycleListener.class);

    @Override
    public void onRuntimeStarted() {
        LOG.trace("Registering {}", TesteeArchiveFactoryImpl.class.getName());
        PersistenceUnitProcessor.setArchiveFactory(new TesteeArchiveFactoryImpl());
    }
}
