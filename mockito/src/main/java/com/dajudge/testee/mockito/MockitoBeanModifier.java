package com.dajudge.testee.mockito;

import com.dajudge.testee.spi.BeanModifier;
import com.dajudge.testee.spi.base.AbstractBaseBeanReplacer;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collection;

import static com.dajudge.testee.utils.ReflectionUtils.fieldsValuesAnnotatedWith;

/**
 * A {@link BeanModifier} for integrating Mockito with TestEE.
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
