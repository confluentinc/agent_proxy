package io.confluent.pas.agent.proxy.registration.schemas;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.kafka.schemaregistry.json.JsonSchemaUtils;
import io.confluent.pas.agent.common.utils.JsonUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The registration schema
 */
@Getter
@NoArgsConstructor
public class RegistrationSchema {
    private String schema;
    private JsonSchema jsonSchema;

    public RegistrationSchema(String schema) {
        this.schema = schema;
        this.jsonSchema = new JsonSchema(schema);
    }

    /**
     * Envelope the payload
     *
     * @param payload The payload
     * @return The enveloped payload
     */
    public JsonNode envelope(Object payload) {
        final JsonNode jsonNode = JsonUtils.toJsonNode(payload);
        return JsonSchemaUtils.envelope(jsonSchema, jsonNode);
    }
}
