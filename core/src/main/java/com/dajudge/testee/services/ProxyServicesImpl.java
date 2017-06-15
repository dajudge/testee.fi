package com.dajudge.testee.services;

import org.jboss.weld.exceptions.WeldException;
import org.jboss.weld.serialization.spi.ProxyServices;

/**
 * Proxy services for Weld.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class ProxyServicesImpl implements ProxyServices {
    // TODO find out more about when this is used.

    @Override
    public ClassLoader getClassLoader(final Class<?> proxiedBeanType) {
        return proxiedBeanType.getClassLoader();
    }

    @Override
    public Class<?> loadBeanClass(final String s) {
        try {
            return Class.forName(s);
        } catch (final ClassNotFoundException e) {
            throw new WeldException("Could not load class " + s, e);
        }
    }

    @Override
    public void cleanup() {

    }
}
