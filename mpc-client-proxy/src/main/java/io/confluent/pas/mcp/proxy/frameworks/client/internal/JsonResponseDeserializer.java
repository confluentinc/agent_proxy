package io.confluent.pas.mcp.proxy.frameworks.client.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.mcp.proxy.frameworks.client.exceptions.AgentException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.everit.json.schema.*;

/**
 * A class to deserialize JSON responses based on the provided schema.
 */
@Slf4j
public class JsonResponseDeserializer {
    /**
     * Enum to represent different JSON types.
     */
    private enum JsonType {
        OBJECT,
        STRING,
        INTEGER,
        BOOLEAN,
        DOUBLE
    }

    private final JsonType jsonType;
    private final AgentConfiguration.ToolConfiguration toolConfiguration;

    /**
     * Constructor to initialize the deserializer with the tool configuration.
     *
     * @param toolConfiguration the tool configuration
     */
    public JsonResponseDeserializer(AgentConfiguration.ToolConfiguration toolConfiguration) {
        this.toolConfiguration = toolConfiguration;
        this.jsonType = getJsonType();
    }

    /**
     * Deserialize the content string into an ObjectNode.
     *
     * @param content the content string to deserialize
     * @return the deserialized ObjectNode
     * @throws AgentException if deserialization fails
     */
    public ObjectNode deserialize(String content) {
        try {
            final JsonNode jsonNode;
            switch (jsonType) {
                case OBJECT -> {
                    // Check if the content starts with '{'
                    if (content.charAt(0) != '{') {
                        log.warn("Response content does not start with '{', wrapping in AgentGenericResponse");
                        content = "{\"response\": \"" + StringEscapeUtils.escapeJson(content) + "\"}";
                    }

                    // Deserialize the content into a JsonNode
                    jsonNode = JsonUtils.toJsonNode(content);
                }
                case STRING -> jsonNode = JsonUtils.toJsonNode(content);
                case INTEGER -> {
                    final Integer value = Integer.parseInt(content);
                    jsonNode = JsonUtils.toJsonNode(value);
                }
                case BOOLEAN -> {
                    final Boolean value = Boolean.parseBoolean(content);
                    jsonNode = JsonUtils.toJsonNode(value);
                }
                case DOUBLE -> {
                    final Double value = Double.parseDouble(content);
                    jsonNode = JsonUtils.toJsonNode(value);
                }
                default -> throw new AgentException("Unsupported JSON type: " + jsonType);
            }

            // Envelope the JsonNode with the output schema
            return JsonSchemaUtils.envelope(toolConfiguration.getOutput_schema(), jsonNode);
        } catch (Exception e) {
            throw new AgentException("Failed to deserialize JSON: " + content, e);
        }
    }

    /**
     * Determine the JSON type based on the schema.
     *
     * @return the JSON type
     */
    private JsonType getJsonType() {
        final Schema schema = toolConfiguration.getOutput_schema().rawSchema();

        if (schema instanceof ObjectSchema) {
            return JsonType.OBJECT;
        } else if (!(schema instanceof ConstSchema) && !(schema instanceof EnumSchema)) {
            return switch (schema) {
                case ArraySchema ignored -> JsonType.OBJECT;
                case CombinedSchema ignored -> JsonType.OBJECT;
                case StringSchema ignored -> JsonType.STRING;
                case NumberSchema numberSchema -> numberSchema.requiresInteger() ? JsonType.INTEGER : JsonType.DOUBLE;
                case null, default -> schema instanceof BooleanSchema ? JsonType.BOOLEAN : JsonType.OBJECT;
            };
        } else {
            return JsonType.OBJECT;
        }
    }
}