package io.confluent.pas.agent.proxy.frameworks.java.models;

import io.confluent.kafka.schemaregistry.annotations.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(value = """
        {
           "properties": {
             "correlationId": {
               "connect.index": 0,
               "type": "string"
             }
           },
           "required": [
             "correlationId"
           ],
           "title": "Record",
           "type": "object"
         }
        """,
        refs = {})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Key {

    private String correlationId;
}
