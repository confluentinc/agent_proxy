package io.confluent.pas.mcp.proxy.frameworks.client.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.mcp.proxy.frameworks.client.exceptions.AgentException;
import org.everit.json.schema.ObjectSchema;
import org.everit.json.schema.StringSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class JsonResponseDeserializerTest {

    @Mock
    private AgentConfiguration.ToolConfiguration toolConfiguration;

    @Mock
    private JsonSchema schema;

    private JsonResponseDeserializer deserializer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(toolConfiguration.getOutput_schema()).thenReturn(schema);
    }

    @Test
    public void testDeserializeObject() {
        when(schema.rawSchema()).thenReturn(ObjectSchema.builder()
                .addPropertySchema("key", new StringSchema())
                .build());

        String content = "{\"key\": \"value\"}";

        deserializer = new JsonResponseDeserializer(toolConfiguration);
        ObjectNode result = deserializer.deserialize(content);

        assertNotNull(result);
        assertEquals("value", result.get("payload").get("key").asText());
    }

    @Test
    public void testDeserializeString() {
        when(schema.rawSchema()).thenReturn(new StringSchema());

        deserializer = new JsonResponseDeserializer(toolConfiguration);

        String content = "\"testString\"";
        JsonNode result = deserializer.deserialize(content);

        assertNotNull(result);
        assertEquals("testString", result.get("payload").asText());
    }

    @Test
    public void testDeserializeInteger() {
        when(schema.rawSchema()).thenReturn(new StringSchema());
        String content = "123";

        deserializer = new JsonResponseDeserializer(toolConfiguration);
        JsonNode result = deserializer.deserialize(content);

        assertNotNull(result);
        assertEquals(123, result.get("payload").asInt());
    }

    @Test
    public void testDeserializeBoolean() {
        when(schema.rawSchema()).thenReturn(new StringSchema());
        String content = "true";

        deserializer = new JsonResponseDeserializer(toolConfiguration);
        JsonNode result = deserializer.deserialize(content);

        assertNotNull(result);
        assertTrue(result.get("payload").asBoolean());
    }

    @Test
    public void testDeserializeDouble() {
        when(schema.rawSchema()).thenReturn(new StringSchema());
        String content = "123.45";

        deserializer = new JsonResponseDeserializer(toolConfiguration);
        JsonNode result = deserializer.deserialize(content);

        assertNotNull(result);
        assertEquals(123.45, result.get("payload").asDouble());
    }

    @Test
    public void testDeserializeInvalidJson() {
        when(schema.rawSchema()).thenReturn(new StringSchema());
        String content = "invalidJson";

        deserializer = new JsonResponseDeserializer(toolConfiguration);
        assertThrows(AgentException.class, () -> deserializer.deserialize(content));
    }
}