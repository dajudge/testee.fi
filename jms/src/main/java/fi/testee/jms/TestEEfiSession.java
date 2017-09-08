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

import fi.testee.jms.message.JmsTextMessage;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import java.io.Serializable;
import java.util.function.Consumer;

public class TestEEfiSession implements Session {
    private final Consumer<ReceivedJmsMessage> messageSink;

    public TestEEfiSession(final Consumer<ReceivedJmsMessage> messageSink) {
        this.messageSink = messageSink;
    }

    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        return notImplemented();
    }

    @Override
    public MapMessage createMapMessage() throws JMSException {
        return notImplemented();
    }

    @Override
    public Message createMessage() throws JMSException {
        return notImplemented();
    }

    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        return notImplemented();
    }

    @Override
    public ObjectMessage createObjectMessage(final Serializable object) throws JMSException {
        return notImplemented();
    }

    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        return notImplemented();
    }

    @Override
    public TextMessage createTextMessage() throws JMSException {
        return new JmsTextMessage();
    }

    @Override
    public TextMessage createTextMessage(final String text) throws JMSException {
        return new JmsTextMessage(text);
    }

    @Override
    public boolean getTransacted() throws JMSException {
        return false;
    }

    @Override
    public int getAcknowledgeMode() throws JMSException {
        return 0;
    }

    @Override
    public void commit() throws JMSException {

    }

    @Override
    public void rollback() throws JMSException {

    }

    @Override
    public void close() throws JMSException {

    }

    @Override
    public void recover() throws JMSException {

    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        return notImplemented();
    }

    @Override
    public void setMessageListener(final MessageListener listener) throws JMSException {
        notImplemented();
    }

    @Override
    public void run() {

    }

    @Override
    public MessageProducer createProducer(final Destination destination) throws JMSException {
        return new TestEEfiMessageProducer(messageSink, destination);
    }

    @Override
    public MessageConsumer createConsumer(final Destination destination) throws JMSException {
        return notImplemented();
    }

    @Override
    public MessageConsumer createConsumer(
            final Destination destination,
            final String messageSelector
    ) throws JMSException {
        return notImplemented();
    }

    @Override
    public MessageConsumer createConsumer(
            final Destination destination,
            final String messageSelector,
            final boolean noLocal
    ) throws JMSException {
        return notImplemented();
    }

    @Override
    public MessageConsumer createSharedConsumer(
            final Topic topic,
            final String sharedSubscriptionName
    ) throws JMSException {
        return notImplemented();
    }

    @Override
    public MessageConsumer createSharedConsumer(
            final Topic topic,
            final String sharedSubscriptionName,
            final String messageSelector
    ) throws JMSException {
        return notImplemented();
    }

    @Override
    public Queue createQueue(String queueName) throws JMSException {
        return notImplemented();
    }

    @Override
    public Topic createTopic(String topicName) throws JMSException {
        return notImplemented();
    }

    @Override
    public TopicSubscriber createDurableSubscriber(
            final Topic topic,
            final String name
    ) throws JMSException {
        return notImplemented();
    }

    @Override
    public TopicSubscriber createDurableSubscriber(
            final Topic topic,
            final String name,
            final String messageSelector,
            final boolean noLocal
    ) throws JMSException {
        return notImplemented();
    }

    @Override
    public MessageConsumer createDurableConsumer(final Topic topic, final String name) throws JMSException {
        return notImplemented();
    }

    @Override
    public MessageConsumer createDurableConsumer(
            final Topic topic,
            final String name,
            final String messageSelector,
            final boolean noLocal
    ) throws JMSException {
        return notImplemented();
    }

    @Override
    public MessageConsumer createSharedDurableConsumer(final Topic topic, final String name) throws JMSException {
        return notImplemented();
    }

    @Override
    public MessageConsumer createSharedDurableConsumer(
            final Topic topic,
            final String name,
            final String messageSelector
    ) throws JMSException {
        return notImplemented();
    }

    @Override
    public QueueBrowser createBrowser(final Queue queue) throws JMSException {
        return notImplemented();
    }

    @Override
    public QueueBrowser createBrowser(final Queue queue, final String messageSelector) throws JMSException {
        return notImplemented();
    }

    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        return notImplemented();
    }

    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        return notImplemented();
    }

    @Override
    public void unsubscribe(final String name) throws JMSException {
        notImplemented();
    }

    private <T> T notImplemented() {
        throw new UnsupportedOperationException("Not implemented in TestEE.fi");
    }
}
