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
package fi.testee.eclipselink;

import fi.testee.spi.RuntimeLifecycleListener;
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
