package io.confluent.pas.mcp.demo.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.*;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

class ToolSpecificationHelper {
    /**
     * Converts the 'Tool' type in the MCP schema to a ToolSpecification object.
     *
     * @param tool The 'Tool' type in the MCP schema.
     * @return The ToolSpecification object.
     * @throws JsonProcessingException If the 'inputSchema' element cannot be parsed.
     */
    static ToolSpecification fromMcpTool(McpSchema.Tool tool) throws JsonProcessingException {
        final ToolSpecification.Builder builder = ToolSpecification.builder();

        builder.name(tool.name());
        builder.description(tool.description());

        final String schema = JsonUtils.toString(tool.inputSchema());
        JsonNode inputSchema = JsonUtils.toJsonNode(schema);

        builder.parameters((JsonObjectSchema) jsonNodeToJsonSchemaElement(inputSchema));

        return builder.build();
    }

    /**
     * Converts the 'inputSchema' element (inside the 'Tool' type in the MCP schema)
     * to a JsonSchemaElement object that describes the tool's arguments.
     */
    static JsonSchemaElement jsonNodeToJsonSchemaElement(JsonNode node) {
        final String nodeType = getType(node);
        if (StringUtils.isEmpty(nodeType)) {
            throw new IllegalArgumentException("Node type is empty: " + node);
        }

        switch (nodeType) {
            case "object" -> {
                JsonObjectSchema.Builder builder = JsonObjectSchema.builder();
                JsonNode required = node.get("required");
                if (required != null) {
                    builder.required(toStringArray((ArrayNode) required));
                }
                if (node.has("additionalProperties")) {
                    builder.additionalProperties(node.get("additionalProperties").asBoolean(false));
                }
                JsonNode description = node.get("description");
                if (description != null) {
                    builder.description(description.asText());
                }
                JsonNode properties = node.get("properties");
                if (properties != null) {
                    ObjectNode propertiesObject = (ObjectNode) properties;
                    for (Map.Entry<String, JsonNode> property : propertiesObject.properties()) {
                        builder.addProperty(property.getKey(), jsonNodeToJsonSchemaElement(property.getValue()));
                    }
                }
                return builder.build();
            }
            case "string" -> {
                if (node.has("enum")) {
                    JsonEnumSchema.Builder builder = JsonEnumSchema.builder();
                    if (node.has("description")) {
                        builder.description(node.get("description").asText());
                    }
                    builder.enumValues(toStringArray((ArrayNode) node.get("enum")));
                    return builder.build();
                } else {
                    JsonStringSchema.Builder builder = JsonStringSchema.builder();
                    if (node.has("description")) {
                        builder.description(node.get("description").asText());
                    }
                    return builder.build();
                }
            }
            case "number" -> {
                JsonNumberSchema.Builder builder = JsonNumberSchema.builder();
                if (node.has("description")) {
                    builder.description(node.get("description").asText());
                }
                return builder.build();
            }
            case "integer" -> {
                JsonIntegerSchema.Builder builder = JsonIntegerSchema.builder();
                if (node.has("description")) {
                    builder.description(node.get("description").asText());
                }
                return builder.build();
            }
            case "boolean" -> {
                JsonBooleanSchema.Builder builder = JsonBooleanSchema.builder();
                if (node.has("description")) {
                    builder.description(node.get("description").asText());
                }
                return builder.build();
            }
            case "array" -> {
                JsonArraySchema.Builder builder = JsonArraySchema.builder();
                if (node.has("description")) {
                    builder.description(node.get("description").asText());
                }
                builder.items(jsonNodeToJsonSchemaElement(node.get("items")));
                return builder.build();
            }
            default -> throw new IllegalArgumentException("Unknown element type: " + nodeType);
        }
    }

    /**
     * Returns the 'type' element of the given JSON node.
     * If the 'type' element is not present, it returns the first non-null type from the 'oneOf' element.
     */
    private static String getType(JsonNode node) {
        if (node.has("type")) {
            return node.get("type").asText();
        }

        // If the 'oneOf' element is present, return the first non-null type
        if (node.has("oneOf")) {
            ArrayNode oneOfArray = (ArrayNode) node.get("oneOf");
            for (JsonNode oneOfNode : oneOfArray) {
                if (oneOfNode.has("type")) {
                    final String type = oneOfNode.get("type").asText();
                    if (!type.equals("null")) {
                        return type;
                    }
                }
            }
        }

        return null;
    }

    private static String[] toStringArray(ArrayNode jsonArray) {
        String[] result = new String[jsonArray.size()];
        for (int i = 0; i < jsonArray.size(); i++) {
            result[i] = jsonArray.get(i).asText();
        }
        return result;
    }
}