package io.confluent.pas.agent.common.services;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializerConfig;
import io.confluent.pas.agent.common.utils.SchemaUtils;
import io.kcache.KafkaCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/* Detailed Class Overview:
 * This RegistrationService class is responsible for managing registration information using a Kafka-based cache.
 * It sets up the necessary Kafka serializers and deserializers for handling JSON schemas, and provides methods to register,
 * unregister, and retrieve registrations. The class also handles schema registration in the Schema Registry when needed.
 */

@Slf4j
public class RegistrationService<K extends Schemas.RegistrationKey, R extends Schemas.Registration> implements Closeable {

    private final Map<K, R> registrationCache;

    /**
     * Constructor for RegistrationService with a handler.
     *
     * @param kafkaConfiguration   the Kafka configuration
     * @param registrationKeyClass the class type of the registration key
     * @param registrationClass    the class type of the registration
     * @param readOnly             whether the service is read-only
     * @param handler              the handler for processing registration updates
     */
    public RegistrationService(KafkaConfiguration kafkaConfiguration,
                               Class<K> registrationKeyClass,
                               Class<R> registrationClass,
                               boolean readOnly,
                               RegistrationServiceHandler.Handler<K, R> handler) {
        this(initialize(kafkaConfiguration,
                registrationKeyClass,
                registrationClass,
                readOnly,
                handler));
    }

    /**
     * Constructor for RegistrationService with a handler.
     *
     * @param registrationCache the registration cache
     */
    public RegistrationService(Map<K, R> registrationCache) {
        this.registrationCache = registrationCache;
    }

    /**
     * Constructor for RegistrationService with a handler.
     *
     * @param kafkaConfiguration   the Kafka configuration
     * @param registrationKeyClass the class type of the registration key
     * @param registrationClass    the class type of the registration
     * @param handler              the handler for processing registration updates
     */
    public RegistrationService(KafkaConfiguration kafkaConfiguration,
                               Class<K> registrationKeyClass,
                               Class<R> registrationClass,
                               RegistrationServiceHandler.Handler<K, R> handler) {
        this(kafkaConfiguration, registrationKeyClass, registrationClass, false, handler);
    }


    /**
     * Constructor for RegistrationService without a handler.
     *
     * @param kafkaConfiguration   the Kafka configuration
     * @param registrationKeyClass the class type of the registration key
     * @param registrationClass    the class type of the registration
     * @param readOnly             whether the service is read-only
     */
    public RegistrationService(KafkaConfiguration kafkaConfiguration,
                               Class<K> registrationKeyClass,
                               Class<R> registrationClass,
                               boolean readOnly) {
        this(kafkaConfiguration, registrationKeyClass, registrationClass, readOnly, null);
    }

    /**
     * Constructor for RegistrationService without a handler.
     *
     * @param kafkaConfiguration   the Kafka configuration
     * @param registrationKeyClass the class type of the registration key
     * @param registrationClass    the class type of the registration
     */
    public RegistrationService(KafkaConfiguration kafkaConfiguration,
                               Class<K> registrationKeyClass,
                               Class<R> registrationClass) {
        this(kafkaConfiguration, registrationKeyClass, registrationClass, false, null);
    }

    /**
     * Close the registration service.
     * Closes the Kafka cache and handles any IO exceptions.
     */
    @Override
    public void close() {
        // Attempt to close the Kafka cache and handle any IO exceptions.
        try {
            if (registrationCache != null && registrationCache instanceof Closeable) {
                ((Closeable) registrationCache).close();
            }
        } catch (IOException e) {
            log.error("Error closing registration cache", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all registrations.
     *
     * @return the list of all registrations
     */
    public List<R> getAllRegistrations() {
        // Retrieve all registration values from the Kafka cache and convert them to a list.
        return registrationCache
                .values()
                .stream()
                .toList();
    }

    /**
     * Check if a registration is already registered.
     *
     * @param key the registration key
     * @return true if the registration is already registered, false otherwise
     */
    public boolean isRegistered(K key) {
        // Check if the provided registration key exists in the Kafka cache.
        return registrationCache.get(key) != null;
    }

    /**
     * Register a new registration.
     *
     * @param key          the registration key
     * @param registration the registration
     */
    public void register(K key, R registration) {
        // Add the provided registration to the Kafka cache using the given key.
        registrationCache.put(key, registration);
    }

    /**
     * Unregister a registration.
     *
     * @param key the registration key
     */
    public void unregister(K key) {
        // Remove the registration associated with the given key from the Kafka cache.
        registrationCache.remove(key);
    }


    /**
     * Initialize the registration service.
     * Configures the Kafka serializers and deserializers, and initializes the Kafka cache.
     *
     * @param kafkaConfiguration   the Kafka configuration
     * @param registrationKeyClass the class type of the registration key
     * @param registrationClass    the class type of the registration
     * @param readOnly             whether the service is read-only
     * @param handler              the handler for processing registration updates
     *                             (optional, can be null)
     * @return the initialized Kafka cache
     */
    private static <K extends Schemas.RegistrationKey, R extends Schemas.Registration> KafkaCache<K, R> initialize(
            KafkaConfiguration kafkaConfiguration,
            Class<K> registrationKeyClass,
            Class<R> registrationClass,
            boolean readOnly,
            RegistrationServiceHandler.Handler<K, R> handler) {
        // Retrieve the Schema Registry configuration settings from KafkaPropertiesFactory.
        final Map<String, Object> srConfig = KafkaPropertiesFactory.getSchemaRegistryConfig(kafkaConfiguration);
        srConfig.put(KafkaJsonSchemaDeserializerConfig.JSON_KEY_TYPE, registrationKeyClass);
        srConfig.put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, registrationClass);
        srConfig.put(KafkaJsonSchemaSerializerConfig.AUTO_REGISTER_SCHEMAS, true);

        // Create and configure the Serde for the registration key using Kafka JSON schema serializer and deserializer.
        final Serde<K> keySerdes = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>()
        );
        keySerdes.configure(srConfig, true);

        // Create and configure the Serde for the registration value using Kafka JSON schema serializer and deserializer.
        final Serde<R> valueSerdes = new Serdes.WrapperSerde<>(
                new KafkaJsonSchemaSerializer<>(),
                new KafkaJsonSchemaDeserializer<>()
        );
        valueSerdes.configure(srConfig, false);

        final RegistrationServiceHandler<K, R> serviceHandler = handler != null
                ? new RegistrationServiceHandler<>(handler)
                : null;

        final KafkaCache<K, R> cache = new KafkaCache<>(
                KafkaPropertiesFactory.getCacheConfig(kafkaConfiguration, readOnly),
                keySerdes,
                valueSerdes,
                serviceHandler,
                null
        );

        cache.init();

        if (serviceHandler != null && serviceHandler.isEmpty()) {
            final String registrationTopic = kafkaConfiguration.registrationTopicName();

            // If a service handler is provided and is empty, register the necessary schemas in the Schema Registry.
            try (SchemaRegistryClient schemaRegistryClient = KafkaPropertiesFactory.getSchemRegistryClient(kafkaConfiguration)) {
                SchemaUtils.registerSchemaIfMissing(registrationTopic, registrationKeyClass, true, schemaRegistryClient);
                SchemaUtils.registerSchemaIfMissing(registrationTopic, registrationClass, false, schemaRegistryClient);
            } catch (Throwable e) {
                log.error("Error registering schemas", e);
            }

        }

        return cache;
    }
}