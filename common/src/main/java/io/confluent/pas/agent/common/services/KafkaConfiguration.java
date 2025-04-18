package io.confluent.pas.agent.common.services;

/**
 * Interface defining the configuration properties required for Kafka connectivity.
 * This interface provides access to essential Kafka and Schema Registry settings
 * with default implementations for some security-related methods.
 */
public interface KafkaConfiguration {

    /**
     * Default security protocol for Kafka connections using SASL over SSL.
     */
    String DEFAULT_SECURITY_PROTOCOL = "SASL_SSL";

    /**
     * Default SASL mechanism for authentication using plain username/password.
     */
    String DEFAULT_SASL_MECHANISM = "PLAIN";

    /**
     * Default topic name used for agent registration.
     */
    String DEFAULT_REGISTRATION_TOPIC_NAME = "_agent_registry";

    /**
     * Default implementation of TopicConfiguration providing basic topic settings.
     */
    class DefaultTopicConfiguration implements TopicConfiguration {
    }

    /**
     * Gets the client ID for Kafka connections.
     *
     * @return A unique identifier for this client instance
     */
    String clientId();

    /**
     * Gets the Kafka broker connection string.
     *
     * @return A comma-separated list of host:port pairs for Kafka brokers
     */
    String brokerServers();

    /**
     * Gets the Schema Registry URL.
     *
     * @return The URL of the Schema Registry service
     */
    String schemaRegistryUrl();

    /**
     * Gets the application identifier.
     *
     * @return A unique identifier for this application instance
     */
    String applicationId();

    /**
     * Gets the security protocol for Kafka connections.
     * Defaults to SASL_SSL if not overridden.
     *
     * @return The security protocol (e.g., PLAINTEXT, SASL_SSL)
     */
    default String securityProtocol() {
        return DEFAULT_SECURITY_PROTOCOL;
    }

    /**
     * Gets the SASL mechanism for authentication.
     * Defaults to PLAIN if not overridden.
     *
     * @return The SASL mechanism type (e.g., PLAIN, SCRAM-SHA-512)
     */
    default String saslMechanism() {
        return DEFAULT_SASL_MECHANISM;
    }

    /**
     * Gets the SASL JAAS configuration.
     *
     * @return The JAAS configuration string for SASL authentication
     */
    String saslJaasConfig();

    /**
     * Gets the Schema Registry authentication credentials.
     *
     * @return The basic auth credentials in format username:password
     */
    String schemaRegistryBasicAuthUserInfo();

    /**
     * Gets the Kafka topic name for registration.
     * Defaults to _agent_registry if not overridden.
     *
     * @return The Kafka topic name for registration
     */
    default String registrationTopicName() {
        return DEFAULT_REGISTRATION_TOPIC_NAME;
    }

    /**
     * Gets the topic configuration settings.
     * Returns a new instance of DefaultTopicConfiguration if not overridden.
     *
     * @return The TopicConfiguration instance
     */
    default TopicConfiguration topicConfiguration() {
        return new DefaultTopicConfiguration();
    }
}