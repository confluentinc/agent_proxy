package io.confluent.pas.agent.common.utils;

import java.util.function.Supplier;

/**
 * Provides support for lazy initialization.
 * This class ensures that an object is only created when it is first needed.
 *
 * @param <T> The type of object that is being lazily initialized.
 */
public class Lazy<T> implements Supplier<T>, AutoCloseable {
    private T instance;
    private final Supplier<T> supplier;

    /**
     * Constructor for Lazy.
     *
     * @param supplier The supplier that creates the instance.
     */
    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    /**
     * Get the lazily initialized instance.
     * If the instance is not yet created, it will be created by the supplier.
     *
     * @return The lazily initialized instance.
     */
    public synchronized T get() {
        if (instance == null) {
            instance = supplier.get();
        }

        return instance;
    }

    /**
     * Check if the instance has been initialized.
     *
     * @return true if the instance is initialized, false otherwise.
     */
    public boolean isInitialized() {
        return instance != null;
    }

    @Override
    public void close() throws Exception {
        if (instance != null && instance instanceof AutoCloseable) {
            ((AutoCloseable) instance).close();
        }
    }
}