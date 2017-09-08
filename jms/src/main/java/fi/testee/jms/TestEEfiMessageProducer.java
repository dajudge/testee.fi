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

import javax.jms.CompletionListener;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import java.util.function.Consumer;

public class TestEEfiMessageProducer implements MessageProducer {
    private final Consumer<ReceivedJmsMessage> messageSink;
    private final Destination destination;

    public TestEEfiMessageProducer(final Consumer<ReceivedJmsMessage> messageSink, final Destination destination) {
        this.messageSink = messageSink;
        this.destination = destination;
    }

    @Override
    public void setDisableMessageID(final boolean value) throws JMSException {
    }

    @Override
    public boolean getDisableMessageID() throws JMSException {
        return false;
    }

    @Override
    public void setDisableMessageTimestamp(final boolean value) throws JMSException {

    }

    @Override
    public boolean getDisableMessageTimestamp() throws JMSException {
        return false;
    }

    @Override
    public void setDeliveryMode(final int deliveryMode) throws JMSException {

    }

    @Override
    public int getDeliveryMode() throws JMSException {
        return 0;
    }

    @Override
    public void setPriority(final int defaultPriority) throws JMSException {

    }

    @Override
    public int getPriority() throws JMSException {
        return 0;
    }

    @Override
    public void setTimeToLive(final long timeToLive) throws JMSException {

    }

    @Override
    public long getTimeToLive() throws JMSException {
        return 0;
    }

    @Override
    public void setDeliveryDelay(final long deliveryDelay) throws JMSException {

    }

    @Override
    public long getDeliveryDelay() throws JMSException {
        return 0;
    }

    @Override
    public Destination getDestination() throws JMSException {
        return null;
    }

    @Override
    public void close() throws JMSException {
    }

    @Override
    public void send(final Message message) throws JMSException {
        send(message, new NullCompletionListener());
    }

    @Override
    public void send(
            final Message message,
            final int deliveryMode,
            final int priority,
            final long timeToLive
    ) throws JMSException {
        send(message, deliveryMode, priority, timeToLive, new NullCompletionListener());
    }

    @Override
    public void send(
            final Destination destination,
            final Message message
    ) throws JMSException {
        throw new UnsupportedOperationException("Destination was specified at creation time");
    }

    @Override
    public void send(
            final Destination destination,
            final Message message,
            final int deliveryMode,
            final int priority,
            long timeToLive
    ) throws JMSException {
        throw new UnsupportedOperationException("Destination was specified at creation time");
    }

    @Override
    public void send(
            final Message message,
            final CompletionListener completionListener
    ) throws JMSException {
        send(message, 0, 0, 0, completionListener);
    }

    @Override
    public void send(
            final Message message,
            final int deliveryMode,
            final int priority,
            final long timeToLive,
            final CompletionListener completionListener
    ) throws JMSException {
        messageSink.accept(new ReceivedJmsMessage(destination, message));
        completionListener.onCompletion(message);
    }

    @Override
    public void send(
            final Destination destination,
            final Message message,
            final CompletionListener completionListener
    ) throws JMSException {
        throw new UnsupportedOperationException("Destination was specified at creation time");
    }

    @Override
    public void send(
            final Destination destination,
            final Message message,
            final int deliveryMode,
            final int priority,
            final long timeToLive,
            final CompletionListener completionListener
    ) throws JMSException {
        throw new UnsupportedOperationException("Destination was specified at creation time");
    }

    private static class NullCompletionListener implements CompletionListener {
        @Override
        public void onCompletion(final Message message) {

        }

        @Override
        public void onException(final Message message, final Exception exception) {

        }
    }
}
