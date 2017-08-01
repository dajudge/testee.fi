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
package fi.testee.rest;

import fi.testee.junit4.TestEEfi;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.inject.Inject;

import static org.junit.Assert.assertNotSame;

@RunWith(TestEEfi.class)
public class LifecycleTest {
    @Inject
    private RestServer restServer;

    private static RestServer remember;

    @Test
    public void is_not_shared_1() {
        if(remember != null) {
            assertNotSame(remember, restServer);
        }
        remember = restServer;
    }

    @Test
    public void is_not_shared_2() {
        is_not_shared_1();
    }
}
