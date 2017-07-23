package com.dajudge.testee.easymock;

import com.dajudge.testee.spi.BeanModifier;
import com.dajudge.testee.spi.BeanModifierFactory;

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
