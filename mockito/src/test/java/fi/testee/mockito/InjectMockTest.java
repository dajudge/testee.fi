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

import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.Test;
import org.mockito.Mock;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InjectMockTest {

    @Test
    public void cdiMock_in_cdiBean_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getCdiMockInCdiViaInject().doIt()));
    }

    @Test
    public void ejbMock_in_cdiBean_via_ejb() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getEjbMockInCdiViaEjb().doIt()));
    }

    @Test
    public void ejbMock_in_cdiBean_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getEjbMockInCdiViaInject().doIt()));
    }

    @Test
    public void cdiMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getCdiMockInEjbViaInject().doIt()));
    }

    @Test
    public void ejbMock_in_ejb_via_ejb() {
        test(it -> assertEquals("lolcats", it.getEjb().getEjbMockInEjbViaEjb().doIt()));
    }

    @Test
    public void ejbMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getEjbMockInEjbViaInject().doIt()));
    }

    private void test(Consumer<TestBean> test) {
        // Given
        final TestSetup testSetup = new TestSetup(TestBean.class, TestRuntime.instance());
        final TestBean testClassInstance = new TestBean();

        // When
        final TestSetup.TestContext context = testSetup.prepareTestInstance("myInstance", testClassInstance, null);
        when(testClassInstance.cdiMock.doIt()).thenReturn("lolcats");
        when(testClassInstance.ejbMock.doIt()).thenReturn("lolcats");

        try {
            // Then
            test.accept(testClassInstance);
        } finally {
            context.shutdown();
        }
    }

    public interface SomeProduct {

    }

    public static class ProducerBean {
        @Produces
        public SomeProduct produce() {
            return mock(SomeProduct.class);
        }
    }

    @Singleton
    public static class ExampleSessionBean1 {
        @Inject
        private ExampleBean2 cdiMockInEjbViaInject;
        @EJB
        private ExampleSessionBean2 ejbMockInEjbViaEjb;
        @Inject
        private ExampleSessionBean2 ejbMockInEjbViaInject;
        @Inject
        private SomeProduct someProduct;

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
        private ExampleBean2 cdiMockInCdiViaInject;
        @EJB
        private ExampleSessionBean2 ejbMockInCdiViaEjb;
        @Inject
        private ExampleSessionBean2 ejbMockInCdiViaInject;

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
        private ExampleBean1 cdiBean;
        @EJB
        private ExampleSessionBean1 ejb;
        @Mock
        private ExampleBean2 cdiMock;
        @Mock
        private ExampleSessionBean2 ejbMock;

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
