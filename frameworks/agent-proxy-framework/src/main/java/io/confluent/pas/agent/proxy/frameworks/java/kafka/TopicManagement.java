package io.confluent.pas.agent.proxy.frameworks.java.kafka;

import io.confluent.kafka.schemaregistry.json.JsonSchema;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;


public interface TopicManagement extends AutoCloseable {

    /**
     * Creates a Kafka topic with the specified name, key class, and value class.
     *
     * @param topicName  the name of the topic to create
     * @param keyClass   the class of the key
     * @param valueClass the class of the value
     * @param <K>        the type of the key
     * @param <V>        the type of the value
     * @throws TopicManagementException if there is an error managing the topic
     * @throws ExecutionException       if there is an error during execution
     * @throws InterruptedException     if the thread is interrupted
     * @throws TimeoutException         if the operation times out
     */
    <K, V> void createTopic(String topicName, Class<K> keyClass, Class<V> valueClass)
            throws TopicManagementException, ExecutionException, InterruptedException, TimeoutException;

    /**
     * Creates a Kafka topic with the specified name, key class, and value schema.
     *
     * @param topicName   the name of the topic to create
     * @param keyClass    the class of the key
     * @param valueSchema the schema of the value
     * @param <K>         the type of the key
     * @throws TopicManagementException if there is an error managing the topic
     * @throws ExecutionException       if there is an error during execution
     * @throws InterruptedException     if the thread is interrupted
     * @throws TimeoutException         if the operation times out
     */
    <K> void createTopic(String topicName, Class<K> keyClass, JsonSchema valueSchema)
            throws TopicManagementException, ExecutionException, InterruptedException, TimeoutException;
    
    /**
     * Closes the topic management resources.
     *
     * @throws Exception if there is an error during closing
     */
    @Override
    void close() throws Exception;
}