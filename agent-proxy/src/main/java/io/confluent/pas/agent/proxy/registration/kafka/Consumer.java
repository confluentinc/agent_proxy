package io.confluent.pas.agent.proxy.registration.kafka;

import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.KafkaPropertiesFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.WakeupException;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumer class for managing Kafka topic subscriptions and message processing.
 *
 * @param <K> Key type for Kafka messages
 * @param <V> Value type for Kafka messages
 */
@Slf4j
public class Consumer<K, V> implements Closeable {

    @FunctionalInterface
    public interface TimeoutChecker {
        void checkTimeouts(long lastCheck);
    }

    private final ExecutorService executorSvc = Executors.newSingleThreadExecutor();
    private final KafkaConsumer<K, V> kafkaConsumer;

    private final ConsumerHandler<K, V> consumerHandler;
    private final List<String> topics = Collections.synchronizedList(new ArrayList<>());
    private final TimeoutChecker timeoutChecker;
    private volatile boolean subscriptionUpdated = false;
    private volatile boolean stopRequested = false;

    /**
     * Constructs a Consumer instance with the specified Kafka configuration and
     * message types.
     *
     * @param kafkaConfiguration Kafka configuration containing connection and auth
     *                           details
     * @param keyClass           Class type for message keys
     * @param requestClass       Class type for message values
     * @param timeoutChecker     The timeout checker to use
     */
    public Consumer(KafkaConfiguration kafkaConfiguration,
            Class<K> keyClass,
            Class<V> requestClass,
            ConsumerHandler<K, V> consumerHandler,
            TimeoutChecker timeoutChecker) {
        this(consumerHandler,
                new KafkaConsumer<>(KafkaPropertiesFactory.getConsumerProperties(
                        kafkaConfiguration,
                        false,
                        keyClass,
                        requestClass)),
                timeoutChecker);
    }

    /**
     * Constructs a Consumer instance with the specified Kafka consumer and handler.
     *
     * @param consumerHandler The handler to process messages
     * @param kafkaConsumer   The Kafka consumer to use
     * @param timeoutChecker  The timeout checker to use
     */
    public Consumer(ConsumerHandler<K, V> consumerHandler,
            KafkaConsumer<K, V> kafkaConsumer,
            TimeoutChecker timeoutChecker) {
        this.kafkaConsumer = kafkaConsumer;
        this.consumerHandler = consumerHandler;
        this.timeoutChecker = timeoutChecker;

        executorSvc.submit(this::runLoop);
    }

    public boolean isSubscribed(String topic) {
        return topics.contains(topic);
    }

    /**
     * Subscribes to a Kafka topic with the specified handler.
     *
     * @param topic The topic to subscribe to
     */
    public void subscribe(String topic) {
        if (topics.contains(topic)) {
            throw new IllegalArgumentException("Subscription already exists for topic: " + topic);
        }

        topics.add(topic);
        subscriptionUpdated = true;
    }

    /**
     * Subscribes to a Kafka topic with the specified handler.
     *
     * @param topicsToAdd The topics to subscribe to
     */
    public void subscribe(Collection<String> topicsToAdd) {
        topics.addAll(topicsToAdd);
        subscriptionUpdated = true;
    }

    /**
     * Unsubscribes from a Kafka topic.
     *
     * @param topic The topic to unsubscribe from
     */
    public void unsubscribe(String topic) {
        topics.remove(topic);
        subscriptionUpdated = true;
    }

    /**
     * Closes the consumer, stopping the polling loop and releasing resources.
     *
     * @throws IOException if an error occurs while closing the consumer
     */
    @Override
    public void close() throws IOException {
        stopRequested = true;

        kafkaConsumer.wakeup();

        try {
            executorSvc.shutdown();

            boolean done = executorSvc.awaitTermination(10, TimeUnit.SECONDS);
            if (!done) {
                log.error("Executor did not terminate");
            }
        } catch (InterruptedException e) {
            log.error("Error waiting for executor to terminate", e);
        }
    }

    /**
     * Main loop for polling Kafka messages and processing them.
     */
    void runLoop() {
        log.info("Starting Consumer Service");

        long lastCheck = System.currentTimeMillis();

        while (!stopRequested) {
            updateSubscriptions();
            if (topics.isEmpty()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.warn("Interrupted while sleeping", e);
                    Thread.currentThread().interrupt();
                }

                continue;
            }

            try {
                var records = kafkaConsumer.poll(Duration.ofMillis(100));

                // Process the records before checking for timeouts
                records.forEach(this::processRecord);

                try {
                    // Check for timeouts in the registered handlers
                    timeoutChecker.checkTimeouts(lastCheck);

                    // Update the last check time
                    lastCheck = System.currentTimeMillis();
                } catch (Exception e) {
                    log.error("Error checking timeouts", e);
                }

            } catch (WakeupException e) {
                if (!stopRequested) {
                    log.error("Unexpected WakeupException", e);
                    throw e;
                }
            }
        }

        kafkaConsumer.close();

        log.info("Consumer Service stopped");
    }

    /**
     * Updates the Kafka topic subscriptions based on the current handlers.
     */
    private void updateSubscriptions() {
        if (subscriptionUpdated) {
            log.info("Updating subscriptions");
            kafkaConsumer.unsubscribe();
            if (!topics.isEmpty()) {
                kafkaConsumer.subscribe(topics);
            } else {
                log.info("No topics to subscribe to");
            }
            subscriptionUpdated = false;
        }
    }

    /**
     * Processes a single Kafka record by invoking the appropriate handler.
     *
     * @param record The Kafka record to process
     */
    void processRecord(ConsumerRecord<K, V> record) {
        if (!topics.contains(record.topic())) {
            log.warn("Received message from unregistered topic: {}", record.topic());
            return;
        }

        try {
            log.info("Processing message from topic: {}", record.topic());
            consumerHandler.onMessage(record.topic(), record.key(), record.value());
        } catch (Exception e) {
            log.error("Failed to process message", e);
        }
    }
}