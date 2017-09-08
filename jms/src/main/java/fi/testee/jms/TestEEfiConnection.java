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
package fi.testee.jms;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import java.util.function.Consumer;

public class TestEEfiConnection implements Connection {
    private final Consumer<ReceivedJmsMessage> messageSink;

    public TestEEfiConnection(final Consumer<ReceivedJmsMessage> messageSink) {
        this.messageSink = messageSink;
    }

    @Override
    public Session createSession(final boolean transacted, final int acknowledgeMode) throws JMSException {
        return new TestEEfiSession(messageSink);
    }

    @Override
    public Session createSession(final int sessionMode) throws JMSException {
        return null;
    }

    @Override
    public Session createSession() throws JMSException {
        return null;
    }

    @Override
    public String getClientID() throws JMSException {
        return null;
    }

    @Override
    public void setClientID(final String clientID) throws JMSException {

    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return null;
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        return null;
    }

    @Override
    public void setExceptionListener(final ExceptionListener listener) throws JMSException {

    }

    @Override
    public void start() throws JMSException {

    }

    @Override
    public void stop() throws JMSException {

    }

    @Override
    public void close() throws JMSException {

    }

    @Override
    public ConnectionConsumer createConnectionConsumer(
            final Destination destination,
            final String messageSelector,
            final ServerSessionPool sessionPool,
            final int maxMessages
    ) throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createSharedConnectionConsumer(
            final Topic topic,
            final String subscriptionName,
            final String messageSelector,
            final ServerSessionPool sessionPool,
            final int maxMessages
    ) throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(
            final Topic topic,
            final String subscriptionName,
            final String messageSelector,
            final ServerSessionPool sessionPool,
            final int maxMessages
    ) throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(
            final Topic topic,
            final String subscriptionName,
            final String messageSelector,
            final ServerSessionPool sessionPool,
            final int maxMessages
    ) throws JMSException {
        return null;
    }
}
