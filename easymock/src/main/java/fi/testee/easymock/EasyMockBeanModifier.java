package fi.testee.easymock;

import fi.testee.spi.BeanModifier;
import fi.testee.spi.base.AbstractBaseBeanReplacer;
import fi.testee.utils.ReflectionUtils;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;

import java.util.Collection;

/**
 * A {@link BeanModifier} for integrating EasyMock with TestEE.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class EasyMockBeanModifier extends AbstractBaseBeanReplacer {

    EasyMockBeanModifier(final Object testSetupClass) {
        super(testSetupClass);
    }

    @Override
    protected Collection<Object> createMocksFor(final Object testClassInstance) {
        EasyMockSupport.injectMocks(testClassInstance);
        return ReflectionUtils.fieldsValuesAnnotatedWith(testClassInstance, Mock.class);
    }
}
