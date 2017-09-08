/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * Copyright 2017 Alex Stockinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package fi.testee.jms.message;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageNotWriteableException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static fi.testee.jms.message.JmsMessagingClientConstants.BOOLEAN;
import static fi.testee.jms.message.JmsMessagingClientConstants.BYTE;
import static fi.testee.jms.message.JmsMessagingClientConstants.DOUBLE;
import static fi.testee.jms.message.JmsMessagingClientConstants.FLOAT;
import static fi.testee.jms.message.JmsMessagingClientConstants.INT;
import static fi.testee.jms.message.JmsMessagingClientConstants.INT_TRUE;
import static fi.testee.jms.message.JmsMessagingClientConstants.LONG;
import static fi.testee.jms.message.JmsMessagingClientConstants.SHORT;
import static fi.testee.jms.message.JmsMessagingClientConstants.STRING;

/**
 * The JmsMessage is the root class of all TestEE.fi JMS messages and implements JMS
 * Message interface.
 */
public class JmsMessage implements Message {

    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    // Default JMS Message properties
    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private int priority = Message.DEFAULT_PRIORITY;
    private long timestamp;
    private boolean redelivered;
    private String correlationID;
    private long expiration = Message.DEFAULT_TIME_TO_LIVE;
    private String messageID;
    private String type;
    private Destination replyTo;
    private Destination destination;
    private long deliveryTime;

    private final Map<String, JMSMessagePropertyValue> properties = new HashMap<String, JMSMessagePropertyValue>();

    /**
     * Gets the message ID.
     * <p>
     * The JMSMessageID header field contains a value that uniquely identifies
     * each message sent by a provider.
     *
     * @return the ID of the message.
     */
    @Override
    public String getJMSMessageID() throws JMSException {
        return messageID;
    }

    /**
     * Sets the message ID.
     * <p>
     * Set when a message is sent. This method can be used to change the value
     * for a message that has been received.
     *
     * @param id The ID of the message.
     */
    @Override
    public void setJMSMessageID(final String id) throws JMSException {
        messageID = id;
    }

    @Override
    public long getJMSTimestamp() throws JMSException {
        return timestamp;
    }

    @Override
    public void setJMSTimestamp(final long timestamp) throws JMSException {
        this.timestamp = timestamp;
    }

    @Override
    public byte[] getJMSCorrelationIDAsBytes() throws JMSException {
        return correlationID != null ? correlationID.getBytes(DEFAULT_CHARSET) : null;
    }

    @Override
    public void setJMSCorrelationIDAsBytes(final byte[] correlationID) throws JMSException {
        try {
            this.correlationID = correlationID != null ? new String(correlationID, "UTF-8") : null;
        } catch (final UnsupportedEncodingException e) {
            throw new JMSException(e.getMessage());
        }
    }

    @Override
    public void setJMSCorrelationID(final String correlationID) throws JMSException {
        this.correlationID = correlationID;
    }

    @Override
    public String getJMSCorrelationID() throws JMSException {
        return correlationID;
    }

    @Override
    public Destination getJMSReplyTo() throws JMSException {
        return replyTo;
    }

    @Override
    public void setJMSReplyTo(final Destination replyTo) throws JMSException {
        this.replyTo = replyTo;
    }

    /**
     * Gets the Destination object for this message.
     * <p>
     * The JMSDestination header field contains the destination to which the
     * message is being sent.
     * <p>
     * When a message is sent, this field is ignored. After completion of the
     * send or publish method, the field holds the destination specified by the
     * method.
     * <p>
     * When a message is received, its JMSDestination value must be equivalent
     * to the value assigned when it was sent.
     *
     * @return The destination of this message.
     */
    @Override
    public Destination getJMSDestination() throws JMSException {
        return destination;
    }

    /**
     * Sets the Destination object for this message.
     * <p>
     * Set when a message is sent. This method can be used to change the value
     * for a message that has been received.
     *
     * @param destination The destination for this message.
     */
    @Override
    public void setJMSDestination(final Destination destination) throws JMSException {
        this.destination = destination;
    }

    @Override
    public int getJMSDeliveryMode() throws JMSException {
        return deliveryMode;
    }

    @Override
    public void setJMSDeliveryMode(final int deliveryMode) throws JMSException {
        this.deliveryMode = deliveryMode;
    }

    @Override
    public boolean getJMSRedelivered() throws JMSException {
        return redelivered;
    }

    @Override
    public void setJMSRedelivered(final boolean redelivered) throws JMSException {
        this.redelivered = redelivered;
    }

    @Override
    public String getJMSType() throws JMSException {
        return type;
    }

    @Override
    public void setJMSType(final String type) throws JMSException {
        this.type = type;
    }

    @Override
    public long getJMSExpiration() throws JMSException {
        return expiration;
    }

    @Override
    public void setJMSExpiration(final long expiration) throws JMSException {
        this.expiration = expiration;
    }

    @Override
    public long getJMSDeliveryTime() throws JMSException {
        return deliveryTime;
    }

    @Override
    public void setJMSDeliveryTime(final long deliveryTime) throws JMSException {
        this.deliveryTime = deliveryTime;
    }

    @Override
    public int getJMSPriority() throws JMSException {
        return priority;
    }

    @Override
    public void setJMSPriority(final int priority) throws JMSException {
        this.priority = priority;
    }

    /**
     * Clears a message's properties and set the write permissions for
     * properties. The message's header fields and body are not cleared.
     */
    @Override
    public void clearProperties() throws JMSException {
        properties.clear();
    }

    /**
     * Indicates whether a property value exists for the given property name.
     *
     * @param name The name of the property.
     * @return true if the property exists.
     */
    @Override
    public boolean propertyExists(final String name) throws JMSException {
        return properties.containsKey(name);
    }

    /**
     * Get the value for a property that represents a java primitive(e.g. int or
     * long).
     *
     * @param property The name of the property to get.
     * @param type     The type of the property.
     * @return the converted value for the property.
     * @throws JMSException           On internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   and NumberFormatException when property name or value is
     *                                null. Method throws same exception as primitives
     *                                corresponding valueOf(String) method.
     */
    <T> T getPrimitiveProperty(final String property, final Class<T> type) throws JMSException {
        if (property == null) {
            throw new NullPointerException("Property name is null");
        }
        final Object value = getObjectProperty(property);
        if (value == null) {
            return handleNullPropertyValue(property, type);
        }
        final T convertedValue = TypeConversionSupport.convert(value, type);
        if (convertedValue == null) {
            throw new MessageFormatException("Property " + property + " was " + value.getClass().getName() +
                    " and cannot be read as " + type.getName());
        }
        return convertedValue;
    }

    @SuppressWarnings("unchecked")
    private <T> T handleNullPropertyValue(final String name, final Class<T> clazz) {
        if (clazz == String.class) {
            return null;
        } else if (clazz == Boolean.class) {
            return (T) Boolean.FALSE;
        } else if (clazz == Double.class || clazz == Float.class) {
            throw new NullPointerException("Value of property with name " + name + " is null.");
        } else {
            throw new NumberFormatException("Value of property with name " + name + " is null.");
        }
    }

    /**
     * Returns the value of the <code>boolean</code> property with the specified
     * name.
     *
     * @param name The name of the property to get.
     * @return the <code>boolean</code> property value for the specified name.
     * @throws JMSException           On internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   When property name is null.
     */
    @Override
    public boolean getBooleanProperty(final String name) throws JMSException {
        return getPrimitiveProperty(name, Boolean.class);
    }

    /**
     * Returns the value of the <code>byte</code> property with the specified
     * name.
     *
     * @param name The name of the property to get.
     * @return the <code>byte</code> property value for the specified name.
     * @throws JMSException           On internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   When property name is null.
     * @throws NumberFormatException  When property value is null.
     */
    @Override
    public byte getByteProperty(final String name) throws JMSException {
        return getPrimitiveProperty(name, Byte.class);
    }

    /**
     * Returns the value of the <code>short</code> property with the specified
     * name.
     *
     * @param name The name of the property to get.
     * @return the <code>short</code> property value for the specified name.
     * @throws JMSException           On internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   When property name is null.
     * @throws NumberFormatException  When property value is null.
     */
    @Override
    public short getShortProperty(final String name) throws JMSException {
        return getPrimitiveProperty(name, Short.class);
    }

    /**
     * Returns the value of the <code>int</code> property with the specified
     * name.
     *
     * @param name The name of the property to get.
     * @return the <code>int</code> property value for the specified name.
     * @throws JMSException           On internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   When property name is null.
     * @throws NumberFormatException  When property value is null.
     */
    @Override
    public int getIntProperty(final String name) throws JMSException {
        return getPrimitiveProperty(name, Integer.class);
    }

    /**
     * Returns the value of the <code>long</code> property with the specified
     * name.
     *
     * @param name The name of the property to get.
     * @return the <code>long</code> property value for the specified name.
     * @throws JMSException           On internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   When property name is null.
     * @throws NumberFormatException  When property value is null.
     */
    @Override
    public long getLongProperty(final String name) throws JMSException {
        return getPrimitiveProperty(name, Long.class);
    }

    /**
     * Returns the value of the <code>float</code> property with the specified
     * name.
     *
     * @param name The name of the property to get.
     * @return the <code>float</code> property value for the specified name.
     * @throws JMSException           Wn internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   When property name or value is null.
     */
    @Override
    public float getFloatProperty(final String name) throws JMSException {
        return getPrimitiveProperty(name, Float.class);
    }

    /**
     * Returns the value of the <code>double</code> property with the specified
     * name.
     *
     * @param name The name of the property to get.
     * @return the <code>double</code> property value for the specified name.
     * @throws JMSException           On internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   When property name or value is null.
     */
    @Override
    public double getDoubleProperty(final String name) throws JMSException {
        return getPrimitiveProperty(name, Double.class);
    }

    /**
     * Returns the value of the <code>String</code> property with the specified
     * name.
     *
     * @param name The name of the property to get.
     * @return the <code>String</code> property value for the specified name.
     * @throws JMSException           On internal error.
     * @throws MessageFormatException If the property cannot be converted to the specified type.
     * @throws NullPointerException   When property name is null.
     */
    @Override
    public String getStringProperty(final String name) throws JMSException {
        return getPrimitiveProperty(name, String.class);
    }

    /**
     * Returns the value of the Java object property with the specified name.
     * <p>
     * This method can be used to return, in boxed format, an object that has
     * been stored as a property in the message with the equivalent
     * <code>setObjectProperty</code> method call, or its equivalent primitive
     * setter method.
     *
     * @param name The name of the property to get.
     * @return the Java object property value with the specified name, in boxed
     * format (for example, if the property was set as an
     * <code>int</code>, an <code>Integer</code> is returned); if there
     * is no property by this name, a null value is returned.
     * @throws JMSException On internal error.
     */
    @Override
    public Object getObjectProperty(final String name) throws JMSException {
        final JMSMessagePropertyValue propertyValue = getJMSMessagePropertyValue(name);
        if (propertyValue != null) {
            return propertyValue.getValue();
        }
        return null;
    }

    /**
     * Returns the property value with message attribute to object property
     * conversions took place.
     * <p>
     *
     * @param name The name of the property to get.
     * @return <code>JMSMessagePropertyValue</code> with object value and
     * corresponding SQS message attribute type and message attribute
     * string value.
     * @throws JMSException On internal error.
     */
    public JMSMessagePropertyValue getJMSMessagePropertyValue(final String name) throws JMSException {
        return properties.get(name);
    }

    private static class PropertyEnum implements Enumeration<String> {
        private final Iterator<String> propertyItr;

        public PropertyEnum(final Iterator<String> propertyItr) {
            this.propertyItr = propertyItr;
        }

        @Override
        public boolean hasMoreElements() {
            return propertyItr.hasNext();
        }

        @Override
        public String nextElement() {
            return propertyItr.next();
        }
    }

    /**
     * Returns an <code>Enumeration</code> of all the property names.
     * <p>
     * Note that JMS standard header fields are not considered properties and
     * are not returned in this enumeration.
     *
     * @return an enumeration of all the names of property values.
     * @throws JMSException On internal error.
     */
    @Override
    public Enumeration<String> getPropertyNames() throws JMSException {
        return new PropertyEnum(properties.keySet().iterator());
    }

    /**
     * Sets a <code>boolean</code> property value with the specified name into
     * the message.
     *
     * @param name  The name of the property to set.
     * @param value The <code>boolean</code> value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setBooleanProperty(final String name, final boolean value) throws JMSException {
        setObjectProperty(name, value);
    }

    /**
     * Sets a <code>byte</code> property value with the specified name into
     * the message.
     *
     * @param name  The name of the property to set.
     * @param value The <code>byte</code> value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setByteProperty(final String name, final byte value) throws JMSException {
        setObjectProperty(name, value);
    }

    /**
     * Sets a <code>short</code> property value with the specified name into
     * the message.
     *
     * @param name  The name of the property to set.
     * @param value The <code>short</code> value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setShortProperty(final String name, final short value) throws JMSException {
        setObjectProperty(name, value);
    }

    /**
     * Sets a <code>int</code> property value with the specified name into
     * the message.
     *
     * @param name  The name of the property to set.
     * @param value The <code>int</code> value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setIntProperty(final String name, final int value) throws JMSException {
        setObjectProperty(name, value);
    }

    /**
     * Sets a <code>long</code> property value with the specified name into
     * the message.
     *
     * @param name  The name of the property to set.
     * @param value The <code>long</code> value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setLongProperty(final String name, final long value) throws JMSException {
        setObjectProperty(name, value);
    }

    /**
     * Sets a <code>float</code> property value with the specified name into
     * the message.
     *
     * @param name  The name of the property to set.
     * @param value The <code>float</code> value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setFloatProperty(final String name, final float value) throws JMSException {
        setObjectProperty(name, value);
    }

    /**
     * Sets a <code>double</code> property value with the specified name into
     * the message.
     *
     * @param name  The name of the property to set.
     * @param value The <code>double</code> value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setDoubleProperty(final String name, final double value) throws JMSException {
        setObjectProperty(name, value);
    }

    /**
     * Sets a <code>String</code> property value with the specified name into
     * the message.
     *
     * @param name  The name of the property to set.
     * @param value The <code>String</code> value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setStringProperty(final String name, final String value) throws JMSException {
        setObjectProperty(name, value);
    }

    /**
     * Sets a Java object property value with the specified name into the
     * message.
     * <p>
     * Note that this method works only for the boxed primitive object types
     * (Integer, Double, Long ...) and String objects.
     *
     * @param name  The name of the property to set.
     * @param value The object value of the property to set.
     * @throws JMSException                 On internal error.
     * @throws IllegalArgumentException     If the name or value is null or empty string.
     * @throws MessageFormatException       If the object is invalid type.
     * @throws MessageNotWriteableException If properties are read-only.
     */
    @Override
    public void setObjectProperty(final String name, final Object value) throws JMSException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Property name can not be null or empty.");
        }
        if (value == null || "".equals(value)) {
            throw new IllegalArgumentException("Property value can not be null or empty.");
        }
        if (!isValidPropertyValueType(value)) {
            throw new MessageFormatException("Value of property with name " + name + " has incorrect type " + value.getClass().getName() + ".");
        }
        properties.put(name, new JMSMessagePropertyValue(value));
    }

    /**
     * Acknowledges message(s).
     *
     * @throws JMSException          On Internal error
     * @throws IllegalStateException If this method is called on a closed session.
     */
    @Override
    public void acknowledge() throws JMSException {
    }

    /**
     * <p>
     * Clears out the message body. Clearing a message's body does not clear its
     * header values or property entries.
     * <p>
     * This method cannot be called directly instead the implementation on the
     * subclasses should be used.
     *
     * @throws JMSException If directly called
     */
    @Override
    public void clearBody() throws JMSException {
        throw new JMSException("JmsMessage does not have any body");
    }

    @Override
    public <T> T getBody(final Class<T> c) throws JMSException {
        return null;
    }

    @Override
    public boolean isBodyAssignableTo(final Class c) throws JMSException {
        return false;
    }

    private boolean isValidPropertyValueType(final Object value) {
        return value instanceof Boolean || value instanceof Byte || value instanceof Short ||
                value instanceof Integer || value instanceof Long || value instanceof Float ||
                value instanceof Double || value instanceof String;
    }

    /**
     * Copied from org.apache.activemq.util.TypeConversionSupport to provide the
     * same property support provided by activemq without creating a dependency
     * on activemq.
     */
    public static class TypeConversionSupport {

        static class ConversionKey {
            final Class<?> from;

            final Class<?> to;

            public ConversionKey(Class<?> from, Class<?> to) {
                this.from = from;
                this.to = to;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((from == null) ? 0 : from.hashCode());
                result = prime * result + ((to == null) ? 0 : to.hashCode());
                return result;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                ConversionKey other = (ConversionKey) obj;
                if (from == null) {
                    if (other.from != null)
                        return false;
                } else if (!from.equals(other.from))
                    return false;
                if (to == null) {
                    if (other.to != null)
                        return false;
                } else if (!to.equals(other.to))
                    return false;
                return true;
            }
        }

        interface Converter {
            Object convert(Object value);
        }

        static final private Map<ConversionKey, Converter> CONVERSION_MAP = new HashMap<ConversionKey, Converter>();

        static {
            Converter toStringConverter = new Converter() {
                public Object convert(Object value) {
                    return value.toString();
                }
            };
            CONVERSION_MAP.put(new ConversionKey(Boolean.class, String.class), toStringConverter);
            CONVERSION_MAP.put(new ConversionKey(Byte.class, String.class), toStringConverter);
            CONVERSION_MAP.put(new ConversionKey(Short.class, String.class), toStringConverter);
            CONVERSION_MAP.put(new ConversionKey(Integer.class, String.class), toStringConverter);
            CONVERSION_MAP.put(new ConversionKey(Long.class, String.class), toStringConverter);
            CONVERSION_MAP.put(new ConversionKey(Float.class, String.class), toStringConverter);
            CONVERSION_MAP.put(new ConversionKey(Double.class, String.class), toStringConverter);

            CONVERSION_MAP.put(new ConversionKey(String.class, Boolean.class), new Converter() {
                public Object convert(Object value) {
                    String stringValue = (String) value;
                    if (Boolean.valueOf(stringValue) || INT_TRUE.equals((String) value)) {
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                }
            });
            CONVERSION_MAP.put(new ConversionKey(String.class, Byte.class), new Converter() {
                public Object convert(Object value) {
                    return Byte.valueOf((String) value);
                }
            });
            CONVERSION_MAP.put(new ConversionKey(String.class, Short.class), new Converter() {
                public Object convert(Object value) {
                    return Short.valueOf((String) value);
                }
            });
            CONVERSION_MAP.put(new ConversionKey(String.class, Integer.class), new Converter() {
                public Object convert(Object value) {
                    return Integer.valueOf((String) value);
                }
            });
            CONVERSION_MAP.put(new ConversionKey(String.class, Long.class), new Converter() {
                public Object convert(Object value) {
                    return Long.valueOf((String) value);
                }
            });
            CONVERSION_MAP.put(new ConversionKey(String.class, Float.class), new Converter() {
                public Object convert(Object value) {
                    return Float.valueOf((String) value);
                }
            });
            CONVERSION_MAP.put(new ConversionKey(String.class, Double.class), new Converter() {
                public Object convert(Object value) {
                    return Double.valueOf((String) value);
                }
            });

            Converter longConverter = new Converter() {
                public Object convert(Object value) {
                    return Long.valueOf(((Number) value).longValue());
                }
            };
            CONVERSION_MAP.put(new ConversionKey(Byte.class, Long.class), longConverter);
            CONVERSION_MAP.put(new ConversionKey(Short.class, Long.class), longConverter);
            CONVERSION_MAP.put(new ConversionKey(Integer.class, Long.class), longConverter);
            CONVERSION_MAP.put(new ConversionKey(Date.class, Long.class), new Converter() {
                public Object convert(Object value) {
                    return Long.valueOf(((Date) value).getTime());
                }
            });

            Converter intConverter = new Converter() {
                public Object convert(Object value) {
                    return Integer.valueOf(((Number) value).intValue());
                }
            };
            CONVERSION_MAP.put(new ConversionKey(Byte.class, Integer.class), intConverter);
            CONVERSION_MAP.put(new ConversionKey(Short.class, Integer.class), intConverter);

            CONVERSION_MAP.put(new ConversionKey(Byte.class, Short.class), new Converter() {
                public Object convert(Object value) {
                    return Short.valueOf(((Number) value).shortValue());
                }
            });

            CONVERSION_MAP.put(new ConversionKey(Float.class, Double.class), new Converter() {
                public Object convert(Object value) {
                    return Double.valueOf(((Number) value).doubleValue());
                }
            });
        }

        @SuppressWarnings("unchecked")
        static public <T> T convert(Object value, Class<T> clazz) {

            assert value != null && clazz != null;

            if (value.getClass() == clazz)
                return (T) value;

            Converter c = (Converter) CONVERSION_MAP.get(new ConversionKey(value.getClass(), clazz));
            if (c == null)
                return null;
            return (T) c.convert(value);

        }
    }

    /**
     * This class is used fulfill object value, corresponding SQS message
     * attribute type and message attribute string value.
     */
    public static class JMSMessagePropertyValue {

        private final Object value;

        private final String type;

        public JMSMessagePropertyValue(final Object value) throws JMSException {
            this.type = getType(value);
            this.value = value;
        }

        private static String getType(final Object value) throws JMSException {
            if (value instanceof String) {
                return STRING;
            } else if (value instanceof Integer) {
                return INT;
            } else if (value instanceof Long) {
                return LONG;
            } else if (value instanceof Boolean) {
                return BOOLEAN;
            } else if (value instanceof Byte) {
                return BYTE;
            } else if (value instanceof Double) {
                return DOUBLE;
            } else if (value instanceof Float) {
                return FLOAT;
            } else if (value instanceof Short) {
                return SHORT;
            } else {
                throw new JMSException("Not a supported JMS property type");
            }
        }

        private static Object getObjectValue(final String value, final String type) throws JMSException {
            if (STRING.equals(type)) {
                return value;
            } else if (INT.equals(type)) {
                return Integer.valueOf(value);
            } else if (LONG.equals(type)) {
                return Long.valueOf(value);
            } else if (BOOLEAN.equals(type)) {
                if (INT_TRUE.equals(value)) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            } else if (BYTE.equals(type)) {
                return Byte.valueOf(value);
            } else if (DOUBLE.equals(type)) {
                return Double.valueOf(value);
            } else if (FLOAT.equals(type)) {
                return Float.valueOf(value);
            } else if (SHORT.equals(type)) {
                return Short.valueOf(value);
            } else {
                throw new JMSException(type + " is not a supported JMS property type");
            }
        }

        Object getValue() {
            return value;
        }
    }
}
