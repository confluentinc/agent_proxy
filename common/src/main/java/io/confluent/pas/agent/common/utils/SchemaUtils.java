package io.confluent.pas.agent.common.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaVersion;
import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import io.confluent.kafka.schemaregistry.client.rest.entities.requests.RegisterSchemaResponse;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Schema utils to generate and register schemas
 */
@Slf4j
public class SchemaUtils {

    private final static Lazy<SchemaGenerator> SCHEMA_GENERATOR = new Lazy<>(() -> new SchemaGenerator(new SchemaGeneratorConfigBuilder(
            SchemaVersion.DRAFT_7,
            OptionPreset.PLAIN_JSON).build()));

    public static void registerSchemaIfMissing(String topicName,
                                               Class<?> clazz,
                                               boolean forKey,
                                               SchemaRegistryClient schemaRegistryClient) {
        try {
            if (!isSchemaRegistered(topicName, forKey, schemaRegistryClient)) {
                registerSchema(topicName, clazz, forKey, schemaRegistryClient);
            }
        } catch (IOException | RestClientException e) {
            log.error("Failed to register schema", e);
        }
    }

    /**
     * Check if a schema is registered
     *
     * @param topicName            Topic name
     * @param forKey               If the schema is for the key
     * @param schemaRegistryClient Schema registry client
     * @return If the schema is registered
     * @throws IOException         If the schema cannot be retrieved
     * @throws RestClientException If the schema cannot be retrieved
     */
    public static boolean isSchemaRegistered(String topicName,
                                             boolean forKey,
                                             SchemaRegistryClient schemaRegistryClient) throws IOException, RestClientException {
        final String subject = topicName + (forKey ? "-key" : "-value");
        try {
            return schemaRegistryClient.getLatestSchemaMetadata(subject) != null;
        } catch (RestClientException e) {
            if (e.getStatus() == 404) {
                return false;
            }

            throw e;
        }
    }

    /**
     * Register a schema for a topic
     *
     * @param topicName            Topic name
     * @param clazz                Class
     * @param forKey               If the schema is for the key
     * @param schemaRegistryClient Schema registry client
     * @throws IOException         If the schema cannot be generated
     * @throws RestClientException If the schema cannot be registered
     */
    public static void registerSchema(String topicName,
                                      Class<?> clazz,
                                      boolean forKey,
                                      SchemaRegistryClient schemaRegistryClient) throws IOException, RestClientException {
        final JsonSchema jsonSchema;
        try {
            jsonSchema = getSchema(clazz, schemaRegistryClient);
        } catch (IOException e) {
            log.error("Failed to generate schema", e);
            throw e;
        }

        registerSchema(topicName, jsonSchema, forKey, schemaRegistryClient);
    }

    /**
     * Register a schema for a topic
     *
     * @param topicName            Topic name
     * @param jsonSchema           Schema
     * @param forKey               If the schema is for the key
     * @param schemaRegistryClient Schema registry client
     * @throws IOException         If the schema cannot be generated
     * @throws RestClientException If the schema cannot be registered
     */
    public static void registerSchema(String topicName,
                                      JsonSchema jsonSchema,
                                      boolean forKey,
                                      SchemaRegistryClient schemaRegistryClient) throws IOException, RestClientException {
        // Then register the schema
        try {
            final String subject = topicName + (forKey ? "-key" : "-value");
            final List<ParsedSchema> parsedSchemas = schemaRegistryClient.getSchemas(subject, false, true);
            if (!parsedSchemas.isEmpty()) {
                log.info("Schema already registered for subject {}", subject);
                return;
            }

            RegisterSchemaResponse response = schemaRegistryClient.registerWithResponse(
                    subject,
                    jsonSchema,
                    false);
            log.info("Registered schema with id {} for subject {}", response.getId(), subject);
        } catch (IOException | RestClientException e) {
            log.error("Failed to register schema", e);
            throw e;
        }
    }

    /**
     * Get the schema for a class
     *
     * @param clazz                Class
     * @param schemaRegistryClient Schema registry client
     * @return JsonSchema
     * @throws IOException If the schema cannot be generated
     */
    public static JsonSchema getSchema(Class<?> clazz, SchemaRegistryClient schemaRegistryClient) throws IOException {
        // First use the annotation to generate the schema
        if (clazz.isAnnotationPresent(Schema.class)) {
            Schema schema = clazz.getAnnotation(Schema.class);
            List<SchemaReference> references = Arrays.stream(schema.refs())
                    .map(ref -> new SchemaReference(ref.name(), ref.subject(), ref.version()))
                    .collect(Collectors.toList());
            return (JsonSchema) schemaRegistryClient.parseSchema(JsonSchema.TYPE, schema.value(), references)
                    .orElseThrow(() -> new IOException("Invalid schema " + schema.value()
                            + " with refs " + references));
        }

        JsonNode jsonNode = SCHEMA_GENERATOR.get().generateSchema(clazz);
        return new JsonSchema(jsonNode);
    }
}
