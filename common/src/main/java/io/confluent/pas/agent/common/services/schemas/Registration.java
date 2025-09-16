package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.*;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Schema(value = """
        {
           "properties":{
              "registrationType":{
                 "connect.index":5,
                 "type":"string"
              },
              "description":{
                 "connect.index":1,
                 "type":"string"
              },
              "name":{
                 "connect.index":0,
                 "type":"string"
              },
              "requestTopicName":{
                 "connect.index":2,
                 "type":"string"
              },
              "responseTopicName":{
                 "connect.index":3,
                 "type":"string"
              },
              "metaData":{
                 "connect.index":4,
                 "type":"object",
                 "additionalProperties":true
              },
              "version":{
                 "connect.index":6,
                 "oneOf":[
                    {
                       "type":"null"
                    },
                    {
                       "type":"string"
                    }
                 ],
                 "default":"N/A"
              }
           },
           "required":[
              "name",
              "description",
              "registrationType",
              "requestTopicName",
              "responseTopicName"
           ],
           "additionalProperties":false,
           "title":"Record",
           "type":"object"
        }""", refs = {})
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "registrationType", defaultImpl = Registration.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ResourceRegistration.class, name = Registration.RESOURCE),
        @JsonSubTypes.Type(value = Registration.class, name = Registration.TOOL)})
@Builder
public class Registration extends AbstractSchema {
    public final static String TOOL = "tool";
    public final static String RESOURCE = "resource";
    public final static String AGENT = "agent";

    @JsonProperty(value = "registrationType", required = true, defaultValue = TOOL)
    private String registrationType;
    @JsonProperty(value = "name", required = true)
    private String name;
    @JsonProperty(value = "description", required = true)
    private String description;
    @JsonProperty(value = "requestTopicName", required = true)
    private String requestTopicName;
    @JsonProperty(value = "responseTopicName", required = true)
    private String responseTopicName;
    @JsonProperty(value = "version", defaultValue = "N/A")
    private String version;

    public Registration(String name,
                        String description,
                        String requestTopicName,
                        String responseTopicName) {
        this(TOOL, name, description, requestTopicName, responseTopicName, "N/A", new HashMap<>());
    }

    public Registration(String registrationType, String name, String description, String requestTopicName, String responseTopicName, String version) {
        super(new HashMap<>());

        this.registrationType = registrationType;
        this.name = name;
        this.description = description;
        this.requestTopicName = requestTopicName;
        this.responseTopicName = responseTopicName;
        this.version = version;
    }

    protected Registration(String registrationType,
                           String name,
                           String description,
                           String requestTopicName,
                           String responseTopicName,
                           String version,
                           Map<String, Object> metaData) {
        super(metaData);

        this.registrationType = registrationType;
        this.name = name;
        this.description = description;
        this.requestTopicName = requestTopicName;
        this.responseTopicName = responseTopicName;
        this.version = version;
    }

    @JsonIgnore
    public boolean isResource() {
        return StringUtils.equals(registrationType, RESOURCE);
    }


}