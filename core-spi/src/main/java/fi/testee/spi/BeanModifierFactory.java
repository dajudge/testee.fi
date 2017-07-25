package fi.testee.spi;

/**
 * Factory for {@link BeanModifier}.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface BeanModifierFactory {
    BeanModifier createBeanModifier(Object testClassInstance);
}
