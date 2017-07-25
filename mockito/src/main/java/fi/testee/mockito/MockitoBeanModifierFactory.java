package fi.testee.mockito;

import fi.testee.spi.BeanModifier;
import fi.testee.spi.BeanModifierFactory;

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
