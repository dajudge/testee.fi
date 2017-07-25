package fi.testee.spi;

/**
 * Interface for TestEE framework lifecycle listeners.
 *
 * @author Alex Stockinger, IT-Stockinger
 */
public interface RuntimeLifecycleListener {
    /**
     * Invoked when the framework starts up.
     */
    void onRuntimeStarted();
}
