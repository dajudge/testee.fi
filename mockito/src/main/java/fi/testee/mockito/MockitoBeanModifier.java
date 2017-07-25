/*
 * Copyright (C) 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
