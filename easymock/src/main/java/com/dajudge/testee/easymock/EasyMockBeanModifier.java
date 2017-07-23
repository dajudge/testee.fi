package com.dajudge.testee.easymock;

import com.dajudge.testee.exceptions.TesteeException;
import com.dajudge.testee.spi.BeanModifier;
import com.dajudge.testee.spi.base.AbstractBaseBeanReplacer;
import com.dajudge.testee.utils.ReflectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;

import java.util.Collection;
import java.util.stream.Collectors;

import static com.dajudge.testee.utils.AnnotationUtils.hasAtLeastOneOf;

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
