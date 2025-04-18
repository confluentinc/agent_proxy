package io.confluent.pas.agent.proxy.frameworks.java.kafka.impl;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.KafkaPropertiesFactory;
import io.confluent.pas.agent.common.services.TopicConfiguration;
import io.confluent.pas.agent.common.utils.Lazy;
import io.confluent.pas.agent.common.utils.SchemaUtils;
import io.confluent.pas.agent.proxy.frameworks.java.kafka.TopicManagement;
import io.confluent.pas.agent.proxy.frameworks.java.kafka.TopicManagementException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.errors.TopicExistsException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
@AllArgsConstructor
public class TopicManagementImpl implements TopicManagement {

    private final SchemaRegistryClient schemaRegistryClient;
    private final TopicConfiguration topicConfiguration;
    private final Lazy<AdminClient> kafkaAdminClient;

    public TopicManagementImpl(KafkaConfiguration kafkaConfigration) {
        this(KafkaPropertiesFactory.getSchemRegistryClient(kafkaConfigration),
                kafkaConfigration.topicConfiguration(),
                new Lazy<>(() -> KafkaAdminClient.create(KafkaPropertiesFactory.getAdminConfig(kafkaConfigration))));
    }

    /**
     * Create a topic with a schema
     *
     * @param topicName  Topic name
     * @param keyClass   Key class
     * @param valueClass Value class
     * @param <V>        Value type
     * @param <K>        Key type
     * @throws TopicManagementException If the topic cannot be created
     * @throws ExecutionException       If the topic creation fails
     * @throws InterruptedException     If the thread is interrupted
     * @throws TimeoutException         If the topic creation times out
     */
    @Override
    public <K, V> void createTopic(String topicName, Class<K> keyClass, Class<V> valueClass)
            throws TopicManagementException, ExecutionException, InterruptedException, TimeoutException {
        // First create the topic
        createTopic(topicName);

        // Then register the schemas
        try {
            SchemaUtils.registerSchema(topicName, keyClass, true, schemaRegistryClient);
            SchemaUtils.registerSchema(topicName, valueClass, false, schemaRegistryClient);
        } catch (IOException | RuntimeException | RestClientException e) {
            log.error("Failed to register schema", e);
            throw new TopicManagementException("Failed to register schema", e);
        }
    }

    /**
     * Create a topic with a schema
     *
     * @param topicName   Topic name
     * @param keyClass    Key class
     * @param valueSchema Value schema
     * @throws TopicManagementException If the topic cannot be created
     * @throws ExecutionException       If the topic creation fails
     * @throws InterruptedException     If the thread is interrupted
     * @throws TimeoutException         If the topic creation times out
     */
    @Override
    public <K> void createTopic(String topicName, Class<K> keyClass, JsonSchema valueSchema)
            throws TopicManagementException, ExecutionException, InterruptedException, TimeoutException {
        // First create the topic
        createTopic(topicName);

        // Then register the schemas
        try {
            SchemaUtils.registerSchema(topicName, keyClass, true, schemaRegistryClient);
            SchemaUtils.registerSchema(topicName, valueSchema, false, schemaRegistryClient);
        } catch (IOException | RuntimeException | RestClientException e) {
            log.error("Failed to register schema", e);
            throw new TopicManagementException("Failed to register schema", e);
        }
    }


    /**
     * Create a topic
     *
     * @param topic Topic name
     * @throws TopicManagementException If the topic cannot be created
     * @throws InterruptedException     If the thread is interrupted
     * @throws ExecutionException       If the topic creation fails
     * @throws TimeoutException         If the topic creation times out
     */
    protected void createTopic(String topic)
            throws TopicManagementException, InterruptedException, ExecutionException, TimeoutException {
        final AdminClient admin = kafkaAdminClient.get();

        log.info("Creating topic {}", topic);

        int numLiveBrokers = admin.describeCluster()
                .nodes()
                .get(topicConfiguration.getTimeout(), TimeUnit.MILLISECONDS)
                .size();
        if (numLiveBrokers == 0) {
            throw new TopicManagementException("No live Kafka brokers");
        }

        int topicReplicationFactor = Math.min(numLiveBrokers, topicConfiguration.getReplicationFactor());
        if (topicReplicationFactor < topicConfiguration.getReplicationFactor()) {
            log.warn("Creating the topic {} using a replication factor of {}, which is less than the desired one of {}.",
                    topic,
                    topicReplicationFactor,
                    topicConfiguration.getReplicationFactor());
        }

        Map<String, String> topicConfigs = topicConfiguration.getConfig();
        topicConfigs.put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);

        final NewTopic topicRequest = new NewTopic(topic, topicConfiguration.getPartitions(), (short) topicReplicationFactor);
        topicRequest.configs(topicConfigs);

        try {
            admin.createTopics(Collections.singleton(topicRequest))
                    .all()
                    .get(topicConfiguration.getTimeout(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TopicExistsException) {
                log.info("Topic {} already exists", topic);
            } else {
                throw e;
            }
        }

    }

    @Override
    public void close() throws Exception {
        if (schemaRegistryClient != null) {
            schemaRegistryClient.close();
        }
        kafkaAdminClient.close();
    }
}