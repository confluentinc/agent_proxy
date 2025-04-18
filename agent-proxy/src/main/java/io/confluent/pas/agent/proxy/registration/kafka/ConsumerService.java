package io.confluent.pas.agent.proxy.registration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.Schemas;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import io.confluent.pas.agent.proxy.registration.kafka.exceptions.TimeoutException;

/**
 * Service that handles responses from Kafka topics by routing messages to
 * appropriate handlers
 * based on correlation IDs. This service:
 * <ul>
 * <li>Subscribes to response topics for registered services</li>
 * <li>Routes messages to the correct handler using correlation IDs</li>
 * <li>Manages the lifecycle of response handlers</li>
 * <li>Handles errors during message processing</li>
 * </ul>
 */
@Slf4j
public class ConsumerService implements Closeable {

    /**
     * Handler interface for processing response messages.
     */
    @FunctionalInterface
    public interface ResponseHandler {
        /**
         * Handles a response message.
         *
         * @param response The response message to handle
         */
        void handle(JsonNode response);
    }

    /**
     * Handler interface for processing errors that occur during message handling.
     */
    @FunctionalInterface
    public interface ErrorHandler {
        /**
         * Handles an error that occurred during message processing.
         *
         * @param error The error that occurred
         */
        void onError(Throwable error);
    }

    /**
     * Record that pairs a response handler with its corresponding error handler.
     *
     * @param responseHandler Handler for processing response messages
     * @param errorHandler    Handler for processing errors that occur during
     *                        message handling
     * @param expiredAt       The timestamp when the handler will expire
     */
    public record RegistrationHandler(
            ResponseHandler responseHandler,
            ErrorHandler errorHandler,
            long expiredAt) {
    }

    /**
     * Record that stores registration information for a specific topic, including
     * the registration details and all handlers indexed by correlation ID.
     *
     * @param registration         The service registration details
     * @param registrationHandlers Map of correlation IDs to their handlers
     */
    public record RegistrationItem(
            Schemas.Registration registration,
            Map<String, RegistrationHandler> registrationHandlers) {
    }

    /**
     * Map of topic names to their registration items, which include handlers
     * indexed by correlation ID.
     */
    @Getter
    private final Map<String, RegistrationItem> responseHandlers = new ConcurrentHashMap<>();

    private final long responseTimeout;

    /**
     * The Kafka consumer used to receive messages.
     */
    private final Consumer<JsonNode, JsonNode> consumer;

    /**
     * Creates a new ConsumerService with the specified Kafka configuration.
     *
     * @param kafkaConfiguration The Kafka configuration to use
     * @param responseTimeout    The maximum time to wait for a response before
     *                           timing out
     */
    public ConsumerService(KafkaConfiguration kafkaConfiguration, long responseTimeout) {
        this.consumer = new Consumer<>(
                kafkaConfiguration,
                JsonNode.class,
                JsonNode.class,
                this::handleResponse,
                this::checkTimeouts);
        this.responseTimeout = responseTimeout;
    }

    /**
     * Creates a new ConsumerService with the specified consumer.
     * Primarily used for testing with mock consumers.
     *
     * @param consumer        The consumer to use
     * @param responseTimeout The maximum time to wait for a response before
     *                        timing out
     */
    public ConsumerService(Consumer<JsonNode, JsonNode> consumer, long responseTimeout) {
        this.consumer = Objects.requireNonNull(consumer, "Consumer must not be null");
        this.responseTimeout = responseTimeout;
    }

    /**
     * Subscribes to the response topics for all provided registrations.
     *
     * @param registrations The collection of registrations to subscribe to
     */
    public void addRegistrations(Collection<Schemas.Registration> registrations) {
        if (registrations == null || registrations.isEmpty()) {
            log.warn("No registrations provided to add");
            return;
        }

        List<String> topics = registrations.stream()
                .map(Schemas.Registration::getResponseTopicName)
                .collect(Collectors.toList());

        log.info("Subscribing to response topics: {}", topics);
        consumer.subscribe(topics);
    }

    /**
     * Registers a handler for responses with a specific correlation ID.
     *
     * @param registration  The service registration details
     * @param correlationId The correlation ID to associate with the handler
     * @param handler       The handler to process responses
     * @param errorHandler  The handler to process errors
     *
     * @throws NullPointerException if any parameter is null
     */
    public void registerResponseHandler(
            Schemas.Registration registration,
            String correlationId,
            ResponseHandler handler,
            ErrorHandler errorHandler) {

        Objects.requireNonNull(registration, "Registration must not be null");
        Objects.requireNonNull(correlationId, "CorrelationId must not be null");
        Objects.requireNonNull(handler, "Handler must not be null");
        Objects.requireNonNull(errorHandler, "ErrorHandler must not be null");

        final String responseTopic = registration.getResponseTopicName();
        final String normalizedCorrelationId = correlationId.toLowerCase();

        log.info("Registering response handler for topic: {} with correlation ID: {}",
                responseTopic, normalizedCorrelationId);

        // Subscribe to the topic if not already subscribed
        if (!consumer.isSubscribed(responseTopic)) {
            log.debug("Subscribing to response topic: {}", responseTopic);
            consumer.subscribe(responseTopic);
        }

        // Add the handler to the map, creating a new registration item if necessary
        responseHandlers.compute(responseTopic, (topic, existingItem) -> {
            if (existingItem == null) {
                log.debug("Creating new registration item for topic: {}", responseTopic);
                existingItem = new RegistrationItem(registration, new ConcurrentHashMap<>());
            }

            Map<String, RegistrationHandler> handlers = existingItem.registrationHandlers();
            if (handlers.containsKey(normalizedCorrelationId)) {
                log.warn("Overwriting existing handler for correlation ID: {}", normalizedCorrelationId);
            }

            final long expiredAt = System.currentTimeMillis() + responseTimeout;
            handlers.put(normalizedCorrelationId,
                    new RegistrationHandler(handler, errorHandler, expiredAt));

            return existingItem;
        });
    }

    /**
     * Closes the consumer and releases resources.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        log.info("Closing ConsumerService");
        consumer.close();
    }

    /**
     * Handles a response message by routing it to the appropriate handler based on
     * the correlation ID.
     * This method is called by the consumer when a message is received.
     *
     * @param topic   The topic the message was received on
     * @param key     The message key (contains correlation ID)
     * @param message The message content
     */
    void handleResponse(String topic, JsonNode key, JsonNode message) {
        if (log.isDebugEnabled()) {
            log.debug("Received response on topic: {}", topic);
        }

        // Find the registration item for this topic
        RegistrationItem registrationItem = responseHandlers.get(topic);
        if (registrationItem == null) {
            log.warn("No registration item found for topic: {}", topic);
            return;
        }

        // Extract the correlation ID
        String correlationIdFieldName = registrationItem.registration.getCorrelationIdFieldName();
        if (!key.has(correlationIdFieldName)) {
            log.warn("Key is missing correlation ID field '{}': {}", correlationIdFieldName, key);
            return;
        }

        // Find and execute the handler
        String correlationId = key.get(correlationIdFieldName).asText().toLowerCase();
        processMessageWithHandler(topic, message, correlationId, registrationItem.registrationHandlers);
    }

    /**
     * Processes a message using the appropriate handler for the given correlation
     * ID.
     *
     * @param topic         The topic the message was received on
     * @param message       The message content
     * @param correlationId The correlation ID
     * @param handlers      Map of correlation IDs to handlers
     */
    private void processMessageWithHandler(
            String topic,
            JsonNode message,
            String correlationId,
            Map<String, RegistrationHandler> handlers) {

        // Find the handler for this correlation ID
        RegistrationHandler handler = handlers.get(correlationId);
        if (handler == null) {
            log.warn("No handler found for correlation ID: {} on topic: {}", correlationId, topic);
            return;
        }

        try {
            // Execute the handler
            log.debug("Executing handler for correlation ID: {}", correlationId);
            handler.responseHandler().handle(message);
            log.debug("Handler execution completed for correlation ID: {}", correlationId);
        } catch (Exception e) {
            log.error("Error processing message with correlation ID: {}", correlationId, e);
            try {
                handler.errorHandler().onError(e);
            } catch (Exception errorHandlingException) {
                log.error("Error handler failed for correlation ID: {}",
                        correlationId, errorHandlingException);
            }
        } finally {
            // Remove the handler after processing
            handlers.remove(correlationId);
            log.debug("Handler removed for correlation ID: {}", correlationId);
        }
    }

    /**
     * Checks for timeouts in the registered handlers.
     *
     * @param lastCheck The last time the timeout was checked
     */
    public void checkTimeouts(long lastCheck) {
        long now = System.currentTimeMillis();

        // Check for timeouts in the registered handlers
        responseHandlers.forEach((topic, registrationItem) -> {
            Map<String, RegistrationHandler> handlers = registrationItem.registrationHandlers();
            List<String> correlationIdsToRemove = new ArrayList<>();

            // Check for timeouts in the registered handlers
            handlers.forEach((correlationId, handler) -> {
                if (handler.expiredAt() > 0 && now > handler.expiredAt()) {
                    log.warn("Timeout for correlation ID: {} on topic: {}", correlationId, topic);

                    handler.errorHandler()
                            .onError(new TimeoutException("Timeout for correlation ID: " + correlationId));
                    correlationIdsToRemove.add(correlationId);
                }
            });

            // Remove the expired handlers
            correlationIdsToRemove.forEach(handlers::remove);
        });
    }
}