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
