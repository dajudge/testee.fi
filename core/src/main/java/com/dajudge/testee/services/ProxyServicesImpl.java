package com.dajudge.testee.services;

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

    }
}
