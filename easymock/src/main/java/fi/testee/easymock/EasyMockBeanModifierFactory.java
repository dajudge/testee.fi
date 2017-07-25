package fi.testee.easymock;

import fi.testee.spi.BeanModifier;
import fi.testee.spi.BeanModifierFactory;

/**
 * Factory for {@link EasyMockBeanModifier}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EasyMockBeanModifierFactory implements BeanModifierFactory {
    @Override
    public BeanModifier createBeanModifier(Object testClassInstance) {
        return new EasyMockBeanModifier(testClassInstance);
    }
}
