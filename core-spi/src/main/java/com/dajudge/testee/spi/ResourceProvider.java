package com.dajudge.testee.spi;

import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Access to container managed resources.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface ResourceProvider {
    Object resolve(InjectionPoint injectionPoint);

    void cleanup();
}
