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

import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.easymock.Mock;
import org.junit.Test;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.function.Consumer;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

public class EasyMockTest {

    @Test
    public void cdiMock_in_cdiBean_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getCdiMockInCdiViaInject().doIt()), 1, 0);
    }

    @Test
    public void ejbMock_in_cdiBean_via_ejb() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getEjbMockInCdiViaEjb().doIt()), 0, 1);
    }

    @Test
    public void ejbMock_in_cdiBean_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getEjbMockInCdiViaInject().doIt()), 0, 1);
    }

    @Test
    public void cdiMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getCdiMockInEjbViaInject().doIt()), 1, 0);
    }

    @Test
    public void ejbMock_in_ejb_via_ejb() {
        test(it -> assertEquals("lolcats", it.getEjb().getEjbMockInEjbViaEjb().doIt()), 0, 1);
    }

    @Test
    public void ejbMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getEjbMockInEjbViaInject().doIt()), 0, 1);
    }

    private void test(final Consumer<TestBean> test, final int cdiMockCount, final int ejbMockCount) {
        // Given
        final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
        final TestBean testClassInstance = new TestBean();

        // When
        final Runnable cleanup = testSetup.prepareTestInstance("myInstance", testClassInstance);
        if (cdiMockCount > 0) {
            expect(testClassInstance.cdiMock.doIt()).andReturn("lolcats").times(cdiMockCount);
        }
        if (ejbMockCount > 0) {
            expect(testClassInstance.ejbMock.doIt()).andReturn("lolcats").times(ejbMockCount);
        }

        try {
            // Then
            replay(testClassInstance.cdiMock, testClassInstance.ejbMock);
            test.accept(testClassInstance);
            verify(testClassInstance.cdiMock, testClassInstance.ejbMock);
        } finally {
            cleanup.run();
        }
    }

    @Singleton
    public static class ExampleSessionBean1 {
        @Inject
        public ExampleBean2 cdiMockInEjbViaInject;
        @EJB
        public ExampleSessionBean2 ejbMockInEjbViaEjb;
        @Inject
        public ExampleSessionBean2 ejbMockInEjbViaInject;

        public ExampleBean2 getCdiMockInEjbViaInject() {
            return cdiMockInEjbViaInject;
        }

        public ExampleSessionBean2 getEjbMockInEjbViaEjb() {
            return ejbMockInEjbViaEjb;
        }

        public ExampleSessionBean2 getEjbMockInEjbViaInject() {
            return ejbMockInEjbViaInject;
        }
    }

    @Singleton
    public static class ExampleSessionBean2 {
        public String doIt() {
            return "I am IronMan";
        }
    }

    public static class ExampleBean2 {
        public String doIt() {
            return "I am IronMan";
        }
    }

    public static class ExampleBean1 {
        @Inject
        public ExampleBean2 cdiMockInCdiViaInject;
        @EJB
        public ExampleSessionBean2 ejbMockInCdiViaEjb;
        @Inject
        public ExampleSessionBean2 ejbMockInCdiViaInject;

        public ExampleBean2 getCdiMockInCdiViaInject() {
            return cdiMockInCdiViaInject;
        }

        public ExampleSessionBean2 getEjbMockInCdiViaEjb() {
            return ejbMockInCdiViaEjb;
        }

        public ExampleSessionBean2 getEjbMockInCdiViaInject() {
            return ejbMockInCdiViaInject;
        }
    }

    public static class TestBean {
        @Inject
        public ExampleBean1 cdiBean;
        @EJB
        public ExampleSessionBean1 ejb;
        @Mock
        public ExampleBean2 cdiMock;
        @Mock
        public ExampleSessionBean2 ejbMock;

        public ExampleBean1 getCdiBean() {
            return cdiBean;
        }

        public ExampleSessionBean1 getEjb() {
            return ejb;
        }

        public ExampleBean2 getCdiMock() {
            return cdiMock;
        }

        public ExampleSessionBean2 getEjbMock() {
            return ejbMock;
        }
    }
}
