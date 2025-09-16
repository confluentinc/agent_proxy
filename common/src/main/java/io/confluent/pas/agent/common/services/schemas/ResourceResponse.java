package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.*;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Schema(value = """
        {
           "properties":{
              "type":{
                 "connect.index":0,
                 "enum":[
                    "text",
                    "blob"
                 ],
                 "default": "text",
                 "type":"string"
              },
              "uri":{
                 "connect.index":1,
                 "type":"string"
              },
              "mimeType":{
                 "connect.index":2,
                 "type":"string"
              },
              "metaData":{
                 "connect.index":3,
                 "type":"object",
                 "additionalProperties":true
              }
           },
           "required":[
              "type",
              "uri",
              "mimeType"
           ],
           "title":"Record",
           "type":"object"
        }""", refs = {})
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = TextResourceResponse.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextResourceResponse.class, name = ResourceResponse.TEXT_TYPE),
        @JsonSubTypes.Type(value = BlobResourceResponse.class, name = ResourceResponse.BLOB_TYPE)})
public class ResourceResponse extends AbstractSchema {
    public static final String TEXT_TYPE = "text";
    public static final String BLOB_TYPE = "blob";

    @JsonProperty(value = "type", required = true)
    private String type;
    @JsonProperty(value = "uri", required = true)
    private String uri;
    @JsonProperty(value = "mimeType", required = true)
    private String mimeType;

    protected ResourceResponse(String type,
                               String uri,
                               String mimeType,
                               Map<String, Object> metaData) {
        super(metaData);

        this.type = type;
        this.uri = uri;
        this.mimeType = mimeType;
    }

    protected ResourceResponse(String type,
                               String uri,
                               String mimeType) {
        super(new HashMap<>());

        this.type = type;
        this.uri = uri;
        this.mimeType = mimeType;
    }
}