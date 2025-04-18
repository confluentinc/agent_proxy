package io.confluent.pas.agent.common.services;

import io.kcache.CacheUpdateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for registration cache updates.
 * This class implements the CacheUpdateHandler interface to handle updates to the registration cache.
 * It accumulates updates until the cache is initialized, then processes them.
 *
 * @param <K> the type of registration key
 * @param <R> the type of registration
 */
@Slf4j
public class RegistrationServiceHandler<K extends Schemas.RegistrationKey, R extends Schemas.Registration> implements CacheUpdateHandler<K, R> {

    /**
     * Interface for handling registration updates.
     *
     * @param <K> the type of registration key
     * @param <R> the type of registration
     */
    public interface Handler<K extends Schemas.RegistrationKey, R extends Schemas.Registration> {
        void handleRegistrations(Map<K, R> registrations);
    }

    private final Handler<K, R> handler;
    private final Map<K, R> accumulator = new HashMap<>();
    private boolean initialized = false;

    @Getter
    private boolean empty;

    /**
     * Constructor for RegistrationServiceHandler.
     *
     * @param handler the handler to process registration updates
     */
    public RegistrationServiceHandler(Handler<K, R> handler) {
        this.handler = handler;
    }

    /**
     * Called when the cache is initialized.
     * Processes any accumulated updates.
     *
     * @param count       the number of entries in the cache
     * @param checkpoints the checkpoints for each topic partition
     */
    @Override
    public void cacheInitialized(int count, Map<TopicPartition, Long> checkpoints) {
        initialized = true;

        if (count == 0) {
            // No event, we might need to register the schemas
            log.info("No registration found in the cache, registering schemas.");
            empty = true;
        } else if (!accumulator.isEmpty()) {
            handler.handleRegistrations(accumulator);
            accumulator.clear();
        }

        log.info("Registration cache initialized.");
    }

    /**
     * Handles updates to the cache.
     * If the cache is not initialized, stores the update in the accumulator.
     * Otherwise, processes the update immediately.
     *
     * @param key      the registration key
     * @param value    the new registration value
     * @param oldValue the old registration value
     * @param tp       the topic partition
     * @param offset   the offset of the update
     * @param ts       the timestamp of the update
     */
    @Override
    public void handleUpdate(K key,
                             R value,
                             R oldValue,
                             TopicPartition tp,
                             long offset,
                             long ts) {
        // If the cache is not initialized, store the registration in the accumulator
        // This will help to keep the last event for each key
        if (initialized) {
            handler.handleRegistrations(new HashMap<>() {{
                put(key, value);
            }});
        } else {
            if (value == null) {
                accumulator.remove(key);
            } else {
                accumulator.put(key, value);
            }
        }
    }
}