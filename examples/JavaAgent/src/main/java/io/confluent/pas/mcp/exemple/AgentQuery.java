package io.confluent.pas.mcp.exemple;

import io.confluent.kafka.schemaregistry.annotations.Schema;

/**
 * AgentQuery class represents a query for the agent.
 * This class is annotated with a schema definition for Kafka Schema Registry.
 */
@Schema(value = """
        {
           "properties": {
             "query": {
               "connect.index": 0,
               "type": "string"
             }
           },
           "required": [
             "query"
           ],
           "title": "Record",
           "type": "object"
         }
        """,
        refs = {})
public record AgentQuery(String query) {
}
