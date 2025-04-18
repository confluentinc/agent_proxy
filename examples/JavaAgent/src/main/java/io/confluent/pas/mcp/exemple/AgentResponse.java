package io.confluent.pas.mcp.exemple;

import io.confluent.kafka.schemaregistry.annotations.Schema;

/**
 * AgentResponse class represents the response from the agent.
 * This class is annotated with a schema definition for Kafka Schema Registry.
 */
@Schema(value = """
        {
           "properties": {
             "sentiment": {
               "connect.index": 0,
               "type": "string"
             }
           },
           "required": [
             "sentiment"
           ],
           "title": "Record",
           "type": "object"
         }
        """,
        refs = {})
public record AgentResponse(String sentiment) {
}
