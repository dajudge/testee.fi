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
package fi.testee.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProxyUtilsTest {
    @Mock
    private Function mock;

    @Test
    public void proxying_works() {
        when(mock.apply("a")).thenReturn("b");
        final Function trace = ProxyUtils.trace(mock, Function.class);
        assertEquals("b", trace.apply("a"));
    }

    static class MyException extends RuntimeException {

    }

    @Test(expected = MyException.class)
    public void proxying_relays_exceptions() {
        when(mock.apply("a")).thenThrow(new MyException());
        final Function trace = ProxyUtils.trace(mock, Function.class);
        trace.apply("a");
    }
}
