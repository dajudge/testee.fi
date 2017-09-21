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
package fi.testee.mocking;

import org.junit.Before;
import org.junit.Test;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public abstract class AbstractBaseMockingTest {

    @Before
    public void setup() {
        TestInterceptor.invocations.clear();
    }

    @Test
    public void cdiMock_in_cdiBean_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getCdiMockInCdiViaInject().doIt()), 1, 0, 0);
        assertIntercepted("ExampleBean1:getCdiMockInCdiViaInject");
    }

    @Test
    public void ejbMock_in_cdiBean_via_ejb() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getEjbMockInCdiViaEjb().doIt()), 0, 1, 0);
    }

    @Test
    public void ejbMock_in_cdiBean_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getEjbMockInCdiViaInject().doIt()), 0, 1, 0);
    }

    @Test
    public void cdiMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getCdiMockInEjbViaInject().doIt()), 1, 0, 0);
    }

    @Test
    public void ejbMock_in_ejb_via_ejb() {
        test(it -> assertEquals("lolcats", it.getEjb().getEjbMockInEjbViaEjb().doIt()), 0, 1, 0);
    }

    @Test
    public void ejbMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getEjbMockInEjbViaInject().doIt()), 0, 1, 0);
    }

    @Test
    public void pureMock_in_cdi_via_inject() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getPureMockInCdiViaInject().doIt()), 0, 0, 1);
    }

    @Test
    public void pureMock_in_cdi_via_ejb() {
        test(it -> assertEquals("lolcats", it.getCdiBean().getPureMockInCdiViaEjb().doIt()), 0, 0, 1);
    }

    @Test
    public void pureMock_in_ejb_via_inject() {
        test(it -> assertEquals("lolcats", it.getEjb().getPureMockInEjbViaInject().doIt()), 0, 0, 1);
    }

    @Test
    public void pureMock_in_ejb_via_ejb() {
        test(it -> assertEquals("lolcats", it.getEjb().getPureMockInEjbViaEjb().doIt()), 0, 0, 1);
    }

    protected abstract void test(
            Consumer<TestBeanInterface> test,
            int cdiMockCount,
            int ejbMockCount,
            int pureMockCount
    );

    private void assertIntercepted(final String fingerprint) {
        assertFalse(
                "No more intercepted invocations when looking for " + fingerprint,
                TestInterceptor.invocations.isEmpty()
        );
        final String actual = TestInterceptor.invocations.remove(0);
        assertEquals(
                "Expected intercepted invocation " + fingerprint + " but found " + actual,
                fingerprint,
                actual
        );
    }

    public interface SomeProduct {

    }

    public static class ProducerBean {
        @Produces
        @UseInterceptor
        public SomeProduct produce() {
            return new SomeProduct() {
            };
        }
    }

    @Singleton
    @UseInterceptor
    public static class ExampleSessionBean1 {
        @Inject
        private ExampleBean2 cdiMockInEjbViaInject;
        @EJB
        private ExampleSessionBean2 ejbMockInEjbViaEjb;
        @Inject
        private ExampleSessionBean2 ejbMockInEjbViaInject;
        @Inject
        private SomeProduct someProduct;
        @Inject
        private NoImplementation pureMockInEjbViaInject;
        @EJB
        private NoImplementation pureMockInEjbViaEjb;

        public ExampleBean2 getCdiMockInEjbViaInject() {
            return cdiMockInEjbViaInject;
        }

        public ExampleSessionBean2 getEjbMockInEjbViaEjb() {
            return ejbMockInEjbViaEjb;
        }

        public ExampleSessionBean2 getEjbMockInEjbViaInject() {
            return ejbMockInEjbViaInject;
        }

        public NoImplementation getPureMockInEjbViaInject() {
            return pureMockInEjbViaInject;
        }

        public NoImplementation getPureMockInEjbViaEjb() {
            return pureMockInEjbViaEjb;
        }
    }

    @Singleton
    @UseInterceptor
    public static class ExampleSessionBean2 {
        public String doIt() {
            return "I am IronMan";
        }
    }

    @UseInterceptor
    public static class ExampleBean2 {
        public String doIt() {
            return "I am IronMan";
        }
    }

    @UseInterceptor
    public static class ExampleBean1 {
        @Inject
        private ExampleBean2 cdiMockInCdiViaInject;
        @EJB
        private ExampleSessionBean2 ejbMockInCdiViaEjb;
        @Inject
        private ExampleSessionBean2 ejbMockInCdiViaInject;
        @Inject
        private NoImplementation pureMockInCdiViaInject;
        @EJB
        private NoImplementation pureMockInCdiViaEjb;
        @Inject
        private InterfaceNotInBeanArchive interfaceNotInBeanArchive;

        public ExampleBean2 getCdiMockInCdiViaInject() {
            return cdiMockInCdiViaInject;
        }

        public ExampleSessionBean2 getEjbMockInCdiViaEjb() {
            return ejbMockInCdiViaEjb;
        }

        public ExampleSessionBean2 getEjbMockInCdiViaInject() {
            return ejbMockInCdiViaInject;
        }

        public NoImplementation getPureMockInCdiViaInject() {
            return pureMockInCdiViaInject;
        }

        public NoImplementation getPureMockInCdiViaEjb() {
            return pureMockInCdiViaEjb;
        }
    }

    public interface NoImplementation {
        String doIt();
    }

    public interface TestBeanInterface {

        ExampleBean1 getCdiBean();

        ExampleSessionBean1 getEjb();
    }

}
