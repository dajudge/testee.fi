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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

@RunWith(TestEEfi.class)
public abstract class AbstractBaseRestTest {
    private final OkHttpClient client = new OkHttpClient();

    @Resource
    private RestServer restServer;

    protected void assertGet(
            final String path,
            final int expectedCode,
            final Consumer<String> bodyAssert
    ) throws IOException {
        final int port = restServer.getPort();
        final Request request = new Request.Builder()
                .url("http://localhost:" + port + path)
                .build();
        final Response response = client.newCall(request).execute();
        try (final ResponseBody body = response.body()) {
            assertEquals(expectedCode, response.code());
            bodyAssert.accept(body.string());
        }
    }
}
