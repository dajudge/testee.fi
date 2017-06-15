package com.dajudge.testee.spi;

import javax.enterprise.inject.spi.Bean;

/**
 * Interface for lifecycle objects of a {@link Plugin} that contains state for a single test instace.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface PluginTestInstance {
    <T> void initializeForBean(Bean<T> bean);
}
