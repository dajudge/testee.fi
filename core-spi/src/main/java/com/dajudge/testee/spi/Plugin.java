package com.dajudge.testee.spi;

/**
 * Interface for a TestEE plugin.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface Plugin {
    /**
     * Sets up the plugin state for a test class.
     *
     * @param testClass the test class to setup the plugin state for.
     * @return the {@link PluginTestSetup} with the state for the test class.
     */
    PluginTestSetup setup(Class<?> testClass);
}
