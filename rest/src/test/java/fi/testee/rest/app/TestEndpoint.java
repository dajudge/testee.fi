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
package fi.testee.rest.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/echo")
public class TestEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(TestEndpoint.class);
    private Session session;

    @Inject
    private ManagedBean managedBean;

    @OnOpen
    public void onOpen(final Session session) {
        LOG.info("Session opened: {}", session.getId());
        this.session = session;
    }

    @OnMessage
    public void onMessage(final String message) throws IOException {
        LOG.info("Received message {} on session {}", message, session.getId());
        try {
            session.getBasicRemote().sendText(managedBean.getValue() + ":" + message);
        } catch (final Exception e) {
            LOG.error("Failed to echo", e);
        }
    }
}
