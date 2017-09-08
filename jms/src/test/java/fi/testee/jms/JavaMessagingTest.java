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
import fi.testee.runtime.TestRuntime;
import fi.testee.runtime.TestSetup;
import org.junit.Test;

import javax.annotation.Resource;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JavaMessagingTest {
    @Test
    public void injects_testQueue() {
        runTest(i -> assertNotNull(i.testQueue));
    }

    @Test
    public void injects_connectionFactory() {
        runTest(i -> assertNotNull(i.connectionFactory));
    }

    @Test
    public void injects_queue() {
        runTest(i -> assertNotNull(i.queue));
    }

    @Test
    public void receives_textMessage_from_queue() {
        runTest(i -> testReception(i, i.queue, d -> {
            assertTrue(d instanceof Queue);
            final Queue receivedQueue = (Queue) d;
            assertEquals("jms/Queue", receivedQueue.getQueueName());
        }));
    }

    @Test
    public void receives_textMessage_from_topic() {
        runTest(i -> testReception(i, i.topic, d -> {
            assertTrue(d instanceof Topic);
            final Topic receivedTopic = (Topic) d;
            assertEquals("jms/Topic", receivedTopic.getTopicName());
        }));
    }

    @Test
    public void sends_textMessage() {
        runTest(i -> {
            i.testQueue.sendMessage("jms/Queue", new JmsTextMessage("Hello world"));
            assertNotNull(i.sink.message);
            assertTrue(i.sink.message instanceof TextMessage);
            assertEquals("Hello world", ((TextMessage) i.sink.message).getText());
        });
    }

    private void testReception(
            final TestClass i,
            final Destination destination, final ThrowingConsumer<Destination> destinationCheck
    ) throws Exception {
        final Connection connection = i.connectionFactory.createConnection();
        assertNotNull(connection);
        final Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        assertNotNull(session);
        final MessageProducer producer = session.createProducer(destination);
        assertNotNull(producer);
        final TextMessage message = session.createTextMessage();
        assertNotNull(message);
        message.setText("I am IronMan");
        producer.send(message);

        final List<ReceivedJmsMessage> messages = i.testQueue.drainReceivedMessages();
        assertEquals(1, messages.size());
        final ReceivedJmsMessage receivedMessage = messages.get(0);
        destinationCheck.accept(receivedMessage.getDestination());
        assertTrue(receivedMessage.getJmsMessage() instanceof TextMessage);
        final TextMessage receivedTextMessage = (TextMessage) receivedMessage.getJmsMessage();
        assertEquals("I am IronMan", receivedTextMessage.getText());
    }

    private void runTest(final ThrowingConsumer<TestClass> test) {
        // Given
        final TestSetup testSetup = new TestSetup(TestClass.class, TestRuntime.instance()).init();
        final TestClass testClassInstance = new TestClass();

        // When
        final TestSetup.TestInstance context = testSetup.prepareTestInstance("myInstance", testClassInstance, null);

        try {
            // Then
            try {
                test.accept(testClassInstance);
            } catch (final AssertionError e) {
                throw e;
            } catch (final Exception e) {
                throw new AssertionError("Unexpected exception", e);
            }
        } finally {
            context.shutdown();
        }
    }

    private interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }

    @Singleton
    public static class Sink {

        public Message message;

        public void message(final Message msg) {
            message = msg;
        }
    }

    @MessageDriven(mappedName = "jms/Queue")
    public static class MessageDrivenBean implements MessageListener {
        @Inject
        private Sink sink;

        @Override
        public void onMessage(final Message msg) {
            sink.message(msg);
        }
    }

    public static class TestClass {
        @Inject
        private Sink sink;
        @Resource(mappedName = "jms/ConnectionFactory")
        private ConnectionFactory connectionFactory;
        @Resource(mappedName = "jms/Queue")
        private Queue queue;
        @Resource(mappedName = "jms/Topic")
        private Topic topic;
        @Resource
        private TestQueue testQueue;
    }
}
