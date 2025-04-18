package io.confluent.pas.agent.proxy.frameworks.java.spring.autoconfig;

import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.TopicConfiguration;
import io.confluent.pas.agent.common.utils.ClientID;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Autoconfiguration class for MCP Proxy Kafka settings.
 * Provides configuration beans for Kafka connection and topic management.
 */
@AutoConfiguration
@AutoConfigureOrder
public class McpProxyAutoConfiguration {

    @Value(("${kafka.client-id:#{null}}"))
    private String clientId;

    /**
     * Comma-separated list of Kafka broker addresses
     */
    @Value("${kafka.broker-servers}")
    private String brokerServers;

    /**
     * Schema Registry URL for Kafka schema management
     */
    @Value("${kafka.sr-url}")
    private String schemaRegistryUrl;

    /**
     * Unique identifier for the Kafka application
     */
    @Value("${kafka.application-id}")
    private String applicationId;

    /**
     * Security protocol for Kafka connection (defaults to KafkaConfiguration.DEFAULT_SECURITY_PROTOCOL)
     */
    @Value("${kafka.security-protocol:" + KafkaConfiguration.DEFAULT_SECURITY_PROTOCOL + "}")
    private String securityProtocol;

    /**
     * SASL mechanism for authentication (defaults to KafkaConfiguration.DEFAULT_SASL_MECHANISM)
     */
    @Value("${kafka.sasl-mechanism:" + KafkaConfiguration.DEFAULT_SASL_MECHANISM + "}")
    private String saslMechanism;

    /**
     * JAAS configuration for Kafka authentication
     */
    @Value("${kafka.jaas-config}")
    private String saslJaasConfig;

    /**
     * Basic authentication credentials for Schema Registry
     */
    @Value("${kafka.sr-basic-auth}")
    private String schemaRegistryBasicAuthUserInfo;

    /**
     * Topic name for registration events (defaults to KafkaConfiguration.DEFAULT_REGISTRATION_TOPIC_NAME)
     */
    @Value("${kafka.registration-topic-name:" + KafkaConfiguration.DEFAULT_REGISTRATION_TOPIC_NAME + "}")
    private String registrationTopicName;

    /**
     * Optional topic-specific configuration settings
     */
    @Value("${kafka.topic-configuration:#{null}}")
    private TopicConfiguration topicConfiguration;

    /**
     * Implementation record for Kafka configuration settings.
     * Provides immutable storage of all Kafka-related configuration parameters.
     */
    public record KafkaConfigurationImpl(String clientId, String brokerServers, String schemaRegistryUrl,
                                         String applicationId,
                                         String securityProtocol, String saslMechanism, String saslJaasConfig,
                                         String schemaRegistryBasicAuthUserInfo, String registrationTopicName,
                                         TopicConfiguration topicConfiguration) implements KafkaConfiguration {

        /**
         * Constructs a new KafkaConfigurationImpl with the specified parameters.
         * Applies default values where necessary and ensures all required fields are properly initialized.
         */
        public KafkaConfigurationImpl(String clientId,
                                      String brokerServers,
                                      String schemaRegistryUrl,
                                      String applicationId,
                                      String securityProtocol,
                                      String saslMechanism,
                                      String saslJaasConfig,
                                      String schemaRegistryBasicAuthUserInfo,
                                      String registrationTopicName,
                                      TopicConfiguration topicConfiguration) {
            this.clientId = StringUtils.isEmpty(clientId) ? ClientID.getOrCreateClientId() : clientId;
            this.brokerServers = brokerServers;
            this.schemaRegistryUrl = schemaRegistryUrl;
            this.applicationId = applicationId;
            this.securityProtocol = getValue(securityProtocol, DEFAULT_SECURITY_PROTOCOL);
            this.saslMechanism = getValue(saslMechanism, DEFAULT_SASL_MECHANISM);
            this.saslJaasConfig = saslJaasConfig;
            this.schemaRegistryBasicAuthUserInfo = schemaRegistryBasicAuthUserInfo;
            this.registrationTopicName = getValue(registrationTopicName, DEFAULT_REGISTRATION_TOPIC_NAME);
            this.topicConfiguration = topicConfiguration == null
                    ? new DefaultTopicConfiguration()
                    : topicConfiguration;
        }

        /**
         * Helper method to handle null or empty values by returning a default value.
         *
         * @param value        The input value to check
         * @param defaultValue The default value to return if input is empty
         * @return The input value if not empty, otherwise the default value
         */
        private static String getValue(final String value, final String defaultValue) {
            return StringUtils.isEmpty(value) ? defaultValue : value;
        }
    }

    /**
     * Creates a KafkaConfiguration bean if none exists.
     * Combines all configuration properties into a single configuration object.
     *
     * @return A new KafkaConfiguration instance with all configured properties
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaConfiguration getKafkaConfiguration() {
        return new KafkaConfigurationImpl(
                clientId,
                brokerServers,
                schemaRegistryUrl,
                applicationId,
                securityProtocol,
                saslMechanism,
                saslJaasConfig,
                schemaRegistryBasicAuthUserInfo,
                registrationTopicName,
                topicConfiguration
        );
    }

    @ConditionalOnMissingBean
    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();

        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }
}