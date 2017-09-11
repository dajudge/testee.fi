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
import fi.testee.rest.app.ManagedBean;
import org.eclipse.jetty.util.component.LifeCycle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import javax.annotation.Resource;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(TestEEfi.class)
public class WebsocketTest {
    @Resource
    private RestServer restServer;
    @Mock
    private ManagedBean managedBean;

    @Test
    public void roundtrip_works() throws Exception {
        when(managedBean.getValue()).thenReturn("mocked");

        final URI uri = URI.create("ws://localhost:" + restServer.getPort() + "/websockets/echo");
        final CountDownLatch latch = new CountDownLatch(1);
        final WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        final Client client = new Client(() -> latch.countDown());
        try (final Session session = container.connectToServer(client, uri)) {
            session.getBasicRemote().sendText("Hello");
            assertTrue(latch.await(5, SECONDS));
        } finally {
            ((LifeCycle) container).stop();
        }
        assertEquals("mocked:Hello", client.message);
    }

    @ClientEndpoint
    public static class Client {
        private final Runnable callback;
        public String message;

        public Client(final Runnable callback) {
            this.callback = callback;
        }

        @OnMessage
        public void onWebSocketText(final String message) throws IOException {
            this.message = message;
            callback.run();
        }

    }
}
