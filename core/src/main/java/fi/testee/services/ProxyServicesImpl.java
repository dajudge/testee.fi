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
package fi.testee.services;

import org.jboss.weld.serialization.spi.ProxyServices;

/**
 * Proxy services for Weld.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class ProxyServicesImpl implements ProxyServices {
    @Override
    public ClassLoader getClassLoader(final Class<?> proxiedBeanType) {
        return proxiedBeanType.getClassLoader();
    }

    @Override
    public Class<?> loadBeanClass(final String s) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void cleanup() {
        // Nothing to be done here
    }
}
