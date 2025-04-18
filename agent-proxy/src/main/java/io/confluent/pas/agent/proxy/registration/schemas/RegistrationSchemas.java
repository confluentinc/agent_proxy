package io.confluent.pas.agent.proxy.registration.schemas;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.common.utils.Lazy;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class RegistrationSchemas {

    private final Lazy<RegistrationSchema> requestKeySchema;
    private final Lazy<RegistrationSchema> requestSchema;
    private final Lazy<RegistrationSchema> responseKeySchema;
    private final Lazy<RegistrationSchema> responseSchema;

    public RegistrationSchema getRequestKeySchema() {
        return requestKeySchema.get();
    }

    public RegistrationSchema getRequestSchema() {
        return requestSchema.get();
    }

    public RegistrationSchema getResponseKeySchema() {
        return responseKeySchema.get();
    }

    public RegistrationSchema getResponseSchema() {
        return responseSchema.get();
    }

    public RegistrationSchemas(SchemaRegistryClient client,
                               Schemas.Registration registration) throws IOException, RestClientException {
        this.requestKeySchema = new Lazy<>(() -> {
            try {
                return getSchema(registration.getRequestTopicName(), true, client);
            } catch (RestClientException | IOException e) {
                log.error("Failed to get request key schema", e);
                throw new RuntimeException(e);
            }
        });
        this.requestSchema = new Lazy<>(() -> {
            try {
                return getSchema(registration.getRequestTopicName(), false, client);
            } catch (RestClientException | IOException e) {
                log.error("Failed to get request schema", e);
                throw new RuntimeException(e);
            }
        });
        this.responseKeySchema = new Lazy<>(() -> {
            try {
                return getSchema(registration.getResponseTopicName(), true, client);
            } catch (RestClientException | IOException e) {
                log.error("Failed to get response key schema", e);
                throw new RuntimeException(e);
            }
        });
        this.responseSchema = new Lazy<>(() -> {
            try {
                return getSchema(registration.getResponseTopicName(), false, client);
            } catch (RestClientException | IOException e) {
                log.error("Failed to get response schema", e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Get the schema for a topic
     *
     * @param topicName The topic name
     * @param forKey    Whether the schema is for the key
     * @return The schema
     * @throws RestClientException If the schema cannot be retrieved
     * @throws IOException         If the schema cannot be retrieved
     */
    private static RegistrationSchema getSchema(String topicName,
                                                boolean forKey,
                                                SchemaRegistryClient schemaRegistryClient) throws RestClientException, IOException {
        final String subject = topicName + (forKey ? "-key" : "-value");
        final String schema = schemaRegistryClient.getLatestSchemaMetadata(subject).getSchema();

        return new RegistrationSchema(schema);
    }
}
