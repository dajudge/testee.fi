package fi.testee.spi;

import javax.enterprise.inject.spi.Bean;

/**
 * Allows modification of Beans.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface BeanModifier {
    <T> void modifyCdiBean(Bean<T> cdiBean);

    <T> SessionBeanFactory<T> modifySessionBean(SessionBeanFactory<T> sessionBean);
}
