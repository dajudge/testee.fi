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
package fi.testee.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ejb.EJB;
import javax.inject.Inject;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertSame;


@ExtendWith(TestEEfi.class)
public class InstantiationTest {
    @EJB
    private SessionBean ejbViaEjb;
    @Inject
    private SessionBean ejbViaInject;
    @Inject
    private ManagedBean cdiViaInject;

    @Test
    public void ejb_via_ejb() {
        assertIsSingleton(() -> ejbViaEjb);
    }

    @Test
    public void ejb_via_inject() {
        assertIsSingleton(() -> ejbViaInject);
    }

    @Test
    public void cdi_via_inject() {
        assertIsSingleton(() -> cdiViaInject);
    }

    private void assertIsSingleton(final Supplier<BaseClass> baseClass) {
        final BaseClass instance = baseClass.get().getThis();
        assertSame(instance, baseClass.get().getThis());
    }

    public static class BaseClass {
        public BaseClass getThis() {
            return this;
        }
    }

    @javax.ejb.Singleton
    public static class SessionBean extends BaseClass {

    }

    @javax.inject.Singleton
    public static class ManagedBean extends BaseClass {

    }
}
