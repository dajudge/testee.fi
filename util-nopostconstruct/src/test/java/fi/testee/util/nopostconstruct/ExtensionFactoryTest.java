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
package fi.testee.util.nopostconstruct;

import fi.testee.util.nopostconstruct.annotation.NoPostConstructFor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExtensionFactoryTest {
    @Mock
    private ProcessAnnotatedType processAnnotatedType;
    @Mock
    private AnnotatedType annotatedType;

    @Before
    public void setupMocks() {
        when(processAnnotatedType.getAnnotatedType()).thenReturn(annotatedType);
    }

    @Test
    public void handles_null_method() {
        assertNull(new ExtensionFactory().create(null));
    }

    @Test
    @NoPostConstructFor(String.class)
    public void handles_annotation() throws NoSuchMethodException {
        final Method method = getClass().getMethod("handles_annotation");
        final NoPostConstructExtension extension = new ExtensionFactory().create(method);
        assertNotNull(extension);

        when(annotatedType.getJavaClass()).thenReturn(String.class);

        extension.processAnnotatedType(processAnnotatedType);

        ArgumentCaptor<AnnotatedType> captor = ArgumentCaptor.forClass(AnnotatedType.class);
        verify(processAnnotatedType).setAnnotatedType(captor.capture());
        final AnnotatedType newType = captor.getValue();
        assertNotNull(newType.getAnnotation(NoPostConstructIntercepted.class));
    }
}
