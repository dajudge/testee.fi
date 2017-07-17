package com.dajudge.testee.mockito;

import com.dajudge.testee.spi.BeanModifier;
import com.dajudge.testee.spi.BeanModifierFactory;

/**
 * Factory for {@link MockitoBeanModifier}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class MockitoBeanModifierFactory implements BeanModifierFactory {
    @Override
    public BeanModifier createBeanModifier(Object testClassInstance) {
        return new MockitoBeanModifier(testClassInstance);
    }
}
