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
package fi.testee;

import fi.testee.interceptor.UseInterceptor;
import org.junit.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;

import static fi.testee.interceptor.TestInterceptor.Type.AROUND_INVOKE;
import static fi.testee.interceptor.TestInterceptor.Type.POST_CONSTRUCT;
import static fi.testee.interceptor.TestInterceptor.Type.PRE_DESTROY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CircularReferenceTest extends BaseDependencyInjectionTest<CircularReferenceTest.TestBean> {
    public CircularReferenceTest() {
        super(TestBean.class);
    }

    @Override
    public void reset() {
        Bean1.instanceCount = 0;
        super.reset();
    }

    @Test
    public void ciruclar_references_work() {
        runTest(() -> {
            final Bean1 bean1 = root.getBean1();
            assertNotNull(bean1);
            final Bean2 bean2 = bean1.getBean2();
            assertNotNull(bean2);
            final Bean1 bean1again = bean2.getBean1();
            assertNotNull(bean1again);
            //assertEquals(1, Bean1.instanceCount);
            //assertEquals(1, Bean2.instanceCount);
            ensureInterception(Bean1.class, null, POST_CONSTRUCT);
            ensureInterception(Bean1.class, "getBean2", AROUND_INVOKE);
            ensureInterception(Bean2.class, null, POST_CONSTRUCT);
            ensureInterception(Bean2.class, "getBean1", AROUND_INVOKE);
        }, () -> {
            ensureInterceptionUnordered(Bean1.class, null, PRE_DESTROY);
            ensureInterceptionUnordered(Bean2.class, null, PRE_DESTROY);
        });
    }

    @Override
    TestBean instance() {
        return new TestBean();
    }

    public static class TestBean {
        @EJB
        private Bean1 bean1;

        public Bean1 getBean1() {
            return bean1;
        }
    }

    @Singleton
    @UseInterceptor
    public static class Bean1 {
        public static int instanceCount;

        @EJB
        private Bean2 bean2;

        public Bean1() {
            instanceCount++;
        }

        @PostConstruct
        public void postConstruct() {
        }

        @PreDestroy
        public void preDestroy() {
        }

        public Bean2 getBean2() {
            return bean2;
        }
    }

    @Singleton
    @UseInterceptor
    public static class Bean2 {
        public static int instanceCount;

        @EJB
        private Bean1 bean1;

        public Bean2() {
            instanceCount++;
        }

        @PostConstruct
        public void postConstruct() {
        }

        @PreDestroy
        public void preDestroy() {
        }

        public Bean1 getBean1() {
            return bean1;
        }
    }
}
