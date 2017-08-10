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

import fi.testee.mocking.spi.AbstractBaseMockContributor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.lang.reflect.Field;

import static fi.testee.utils.ReflectionUtils.hasAnnotation;

public class MockitoMockContributor extends AbstractBaseMockContributor {
    @Override
    protected boolean isMockField(final Field field) {
        return hasAnnotation(Spy.class, Mock.class).test(field);
    }

    @Override
    protected void injectMocks(final Object testInstance) {
        MockitoAnnotations.initMocks(testInstance);
    }
}
