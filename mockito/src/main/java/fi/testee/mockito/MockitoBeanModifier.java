package fi.testee.mockito;

import fi.testee.spi.BeanModifier;
import fi.testee.spi.base.AbstractBaseBeanReplacer;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collection;

import static fi.testee.utils.ReflectionUtils.fieldsValuesAnnotatedWith;

/**
 * A {@link BeanModifier} for integrating Mockito with TestEE.fi.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public class MockitoBeanModifier extends AbstractBaseBeanReplacer {

    MockitoBeanModifier(final Object testSetupClass) {
        super(testSetupClass);
    }

    @Override
    protected Collection<Object> createMocksFor(final Object testClassInstance) {
        MockitoAnnotations.initMocks(testClassInstance);
        return fieldsValuesAnnotatedWith(testClassInstance, Mock.class, Spy.class);
    }

}
