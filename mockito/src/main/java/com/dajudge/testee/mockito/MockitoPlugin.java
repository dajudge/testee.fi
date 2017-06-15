package com.dajudge.testee.mockito;

import com.dajudge.testee.spi.Plugin;
import com.dajudge.testee.spi.PluginTestInstance;
import com.dajudge.testee.spi.PluginTestSetup;

/**
 * {@link Plugin} for integrating mockito with TestEE.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class MockitoPlugin implements Plugin {
    @Override
    public PluginTestSetup setup(Class<?> testClass) {
        return new PluginTestSetup() {
            @Override
            public PluginTestInstance instantiate(final Object testClassInstance) {
                return new MockitoTestInstance(testClassInstance);
            }

            @Override
            public void shutdown() {

            }
        };
    }
}
