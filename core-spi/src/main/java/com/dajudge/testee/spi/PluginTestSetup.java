package com.dajudge.testee.spi;

/**
 * Interface for lifecycle objects of a {@link Plugin} that contains state for test setups.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface PluginTestSetup {
    /**
     * Instantiates a {@link PluginTestInstance} for a test class instance.
     *
     * @param testClassInstance the test class instance.
     * @return the {@link PluginTestInstance}.
     */
    PluginTestInstance instantiate(Object testClassInstance);

    /**
     * Releases all resources associated with state for this test setup.
     */
    void shutdown();
}
