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

import fi.testee.mocking.AbstractBaseMockingTest;
import fi.testee.mocking.InterfaceNotInBeanArchive;
import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.mockito.Mock;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.function.Consumer;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MockitoTest extends AbstractBaseMockingTest {

    @Override
    protected void test(
            final Consumer<TestBeanInterface> test,
            final int cdiMockCount,
            final int ejbMockCount,
            final int pureMockCount
    ) {
        // Given
        final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance()).init();
        final TestBean testClassInstance = new TestBean();

        // When
        final TestSetup.TestInstance context = testSetup.prepareTestInstance("myInstance", testClassInstance, null);
        when(testClassInstance.cdiMock.doIt()).thenReturn("lolcats");
        when(testClassInstance.ejbMock.doIt()).thenReturn("lolcats");
        when(testClassInstance.noImplementation.doIt()).thenReturn("lolcats");

        try {
            // Then
            test.accept(testClassInstance);
        } finally {
            context.shutdown();
        }

        verify(testClassInstance.cdiMock, times(cdiMockCount)).doIt();
        verify(testClassInstance.ejbMock, times(ejbMockCount)).doIt();
        verify(testClassInstance.noImplementation, times(pureMockCount)).doIt();
    }

    public static class TestBean implements TestBeanInterface {
        @Inject
        private ExampleBean1 cdiBean;
        @EJB
        private ExampleSessionBean1 ejb;
        @Mock
        private ExampleBean2 cdiMock;
        @Mock
        private ExampleSessionBean2 ejbMock;
        @Mock
        private NoImplementation noImplementation;
        @Mock
        private InterfaceNotInBeanArchive someBaseInterface;

        @Override
        public ExampleBean1 getCdiBean() {
            return cdiBean;
        }

        @Override
        public ExampleSessionBean1 getEjb() {
            return ejb;
        }
    }
}
