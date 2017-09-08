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

import fi.testee.spi.DependencyInjection;
import fi.testee.spi.Releaser;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TestQueueImpl implements TestQueue {
    @Inject
    private MessageDrivenBeanRegistry messageDrivenBeanRegistry;
    @Resource(mappedName = "testeefi/instance/dependencyInjection")
    private DependencyInjection dependencyInjection;

    private final List<ReceivedJmsMessage> messages = new ArrayList<>();

    @Override
    public void sendMessage(final String destination, final Message message) {
        final Class<? extends MessageListener> mdbClass = messageDrivenBeanRegistry.get(destination);
        final Releaser releaser = new Releaser();
        final MessageListener mdb = dependencyInjection.getInstanceOf(mdbClass, releaser);
        try {
            mdb.onMessage(message);
        } finally {
            releaser.release();
        }
    }

    @Override
    public List<ReceivedJmsMessage> drainReceivedMessages() {
        final List<ReceivedJmsMessage> ret = new ArrayList<>();
        ret.addAll(messages);
        messages.clear();
        return ret;
    }

    public void addMessage(final ReceivedJmsMessage msg) {
        messages.add(msg);
    }
}
