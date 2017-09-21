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

import fi.testee.mocking.AbstractBaseMockingTest;
import fi.testee.mocking.InterfaceNotInBeanArchive;
import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.easymock.Mock;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.function.Consumer;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

public class EasyMockTest extends AbstractBaseMockingTest {

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
        if (cdiMockCount > 0) {
            expect(testClassInstance.cdiMock.doIt()).andReturn("lolcats").times(cdiMockCount);
        }
        if (ejbMockCount > 0) {
            expect(testClassInstance.ejbMock.doIt()).andReturn("lolcats").times(ejbMockCount);
        }
        if (pureMockCount > 0) {
            expect(testClassInstance.noImplementation.doIt()).andReturn("lolcats").times(pureMockCount);
        }

        try {
            // Then
            replay(testClassInstance.cdiMock, testClassInstance.ejbMock, testClassInstance.noImplementation);
            test.accept(testClassInstance);
            verify(testClassInstance.cdiMock, testClassInstance.ejbMock, testClassInstance.noImplementation);
        } finally {
            context.shutdown();
        }
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
