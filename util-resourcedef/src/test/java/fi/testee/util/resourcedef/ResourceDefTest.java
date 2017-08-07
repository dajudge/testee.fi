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
package fi.testee.util.resourcedef;

import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.Test;
import org.mockito.Mock;

import javax.annotation.Resource;

import static org.junit.Assert.assertSame;

public class ResourceDefTest {

    @Test
    public void resourceDef_works() {
        final TestSetup setup = new TestSetup(TestClass.class, TestRuntime.instance()).init();
        final TestClass instance = new TestClass();
        final TestSetup.TestInstance context = setup.prepareTestInstance("myTest", instance, null);
        try {
            assertSame(instance.mock, instance.injected);
        } finally {
            context.shutdown();
            setup.shutdown();
        }
    }

    public interface SomeInterface {

    }

    public static class TestClass {
        @ResourceDef
        @Mock
        private SomeInterface mock;
        @Resource
        private SomeInterface injected;
    }
}
