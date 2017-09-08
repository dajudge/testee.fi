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
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.util.function.Consumer;

class TestEEfiConnectionFactory implements ConnectionFactory {

    private final Consumer<ReceivedJmsMessage> messageSink;

    TestEEfiConnectionFactory(final Consumer<ReceivedJmsMessage> messageSink) {
        this.messageSink = messageSink;
    }

    @Override
    public Connection createConnection() throws JMSException {
        return new TestEEfiConnection(messageSink);
    }

    @Override
    public Connection createConnection(final String userName, final String password) throws JMSException {
        return createConnection();
    }

    @Override
    public JMSContext createContext() {
        return null;
    }

    @Override
    public JMSContext createContext(final String userName, final String password) {
        return null;
    }

    @Override
    public JMSContext createContext(final String userName, final String password, final int sessionMode) {
        return null;
    }

    @Override
    public JMSContext createContext(final int sessionMode) {
        return null;
    }
}
