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

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class StaticTest extends AbstractBaseRestTest {
    @Test
    public void serves_resources() throws IOException {
        assertGet("/static/test.txt", 200, body -> assertEquals("TestContent", body));
    }

    @Test
    public void notFound_when_not_found() throws IOException {
        assertGet("/static/notFound", 404, body -> {
        });
    }
}
