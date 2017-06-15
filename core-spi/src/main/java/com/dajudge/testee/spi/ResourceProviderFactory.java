package com.dajudge.testee.spi;

/**
 * Interface for factories for {@link ResourceProvider resource providers}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface ResourceProviderFactory {
    ResourceProvider create(Class<?> setupClass);
}
