package com.dajudge.testee.spi;

import javax.enterprise.inject.spi.Bean;

/**
 * Allows modification of Beans.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface BeanModifier {
    <T> void initializeForBean(Bean<T> bean);
}
