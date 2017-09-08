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

import fi.testee.exceptions.TestEEfiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.MessageDriven;
import javax.inject.Singleton;
import javax.jms.MessageListener;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class MessageDrivenBeanRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(MessageDrivenBeanRegistry.class);

    private final Map<String, Class<? extends MessageListener>> messageDrivenBeans = new HashMap<>();

    public <X extends  MessageListener> void register(final MessageDriven annotation, final Class<X> javaClass) {
        LOG.info("{} -> {}", annotation.mappedName(), javaClass.getName());
        messageDrivenBeans.put(annotation.mappedName(), javaClass);
    }

    public Class<? extends MessageListener> get(final String destination) {
        if (!messageDrivenBeans.containsKey(destination)) {
            throw new TestEEfiException("No @MessageDriven bean found for destination " + destination);
        }
        return messageDrivenBeans.get(destination);
    }
}
