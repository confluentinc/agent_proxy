package io.confluent.pas.agent.common.services;

import com.fasterxml.jackson.annotation.*;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.confluent.pas.agent.common.utils.UriUtils;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class Schemas {

    @Schema(value = """
            {
               "properties":{
                  "registrationType":{
                     "connect.index":5,
                     "type":"string"
                  },
                  "correlationIdFieldName":{
                     "connect.index":4,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
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
                  "mimeType":{
                     "connect.index":6,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  },
                  "url":{
                     "connect.index":7,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
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
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "registrationType", defaultImpl = Registration.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ResourceRegistration.class, name = Registration.RESOURCE),
            @JsonSubTypes.Type(value = Registration.class, name = Registration.TOOL)})
    @Builder
    public static class Registration {
        public final static String TOOL = "tool";
        public final static String RESOURCE = "resource";
        public final static String CORRELATION_ID_FIELD_NAME = "correlationId";

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
        @JsonProperty(value = "correlationIdFieldName", defaultValue = CORRELATION_ID_FIELD_NAME)
        private String correlationIdFieldName;

        public Registration(String name, String description, String requestTopicName, String responseTopicName) {
            this(TOOL, name, description, requestTopicName, responseTopicName, CORRELATION_ID_FIELD_NAME);
        }

        public Registration(String name, String description, String requestTopicName, String responseTopicName, String correlationIdFieldName) {
            this(TOOL, name, description, requestTopicName, responseTopicName, correlationIdFieldName);
        }

        @JsonIgnore
        public boolean isResource() {
            return StringUtils.equals(registrationType, RESOURCE);
        }
    }

    @Setter
    @Getter
    @Schema(value = """
            {
               "properties":{
                  "registrationType":{
                     "connect.index":5,
                     "type":"string"
                  },
                  "correlationIdFieldName":{
                     "connect.index":4,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
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
                  "mimeType":{
                     "connect.index":6,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  },
                  "url":{
                     "connect.index":7,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
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
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceRegistration extends Registration {
        private String mimeType;
        private String url;

        public void setUrl(String url) {
            if (StringUtils.isBlank(url)) {
                throw new IllegalArgumentException("url cannot be blank");
            }

            this.url = url.startsWith("/") ? url.substring(1) : url;
        }

        @JsonIgnore
        public boolean isTemplate() {
            return UriUtils.isTemplate(url);
        }

        public ResourceRegistration(String name,
                                    String description,
                                    String requestTopicName,
                                    String responseTopicName,
                                    String correlationIdFieldName,
                                    String mimeType,
                                    String url) {
            super(RESOURCE, name, description, requestTopicName, responseTopicName, correlationIdFieldName);
            this.mimeType = mimeType;
            setUrl(url);
        }

        public ResourceRegistration(String name,
                                    String description,
                                    String requestTopicName,
                                    String responseTopicName,
                                    String mimeType,
                                    String url) {
            super(RESOURCE, name, description, requestTopicName, responseTopicName, CORRELATION_ID_FIELD_NAME);
            this.mimeType = mimeType;
            setUrl(url);
        }
    }


    /**
     * A key for a registration.
     */
    @Schema(value = """
            {
               "properties": {
                 "name": {
                   "connect.index": 0,
                   "type": "string"
                 }
               },
               "required": [
                 "name"
               ],
               "title": "Record",
               "type": "object"
             }
            """,
            refs = {})
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @AllArgsConstructor()
    @NoArgsConstructor()
    public static class RegistrationKey implements Comparable<RegistrationKey> {

        @JsonProperty(value = "name", required = true)
        private String name;

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this || obj instanceof RegistrationKey key && StringUtils.equals(name, key.getName());
        }

        @Override
        public int compareTo(@NotNull RegistrationKey o) {
            return StringUtils.compare(name, o.getName());
        }
    }

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
                  "text":{
                     "connect.index":3,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  },
                  "blob":{
                     "connect.index":4,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
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
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = TextResourceResponse.class)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TextResourceResponse.class, name = "text"),
            @JsonSubTypes.Type(value = BlobResourceResponse.class, name = "blob")})
    public static class ResourceResponse {
        public enum ResponseType {
            TEXT("text"),
            BLOB("blob");

            private final String value;

            ResponseType(String value) {
                this.value = value;
            }

            @JsonValue
            public String getValue() {
                return value;
            }

            @Override
            public String toString() {
                return value;
            }

            @JsonCreator
            public static ResourceResponse.ResponseType fromValue(String value) {
                for (ResourceResponse.ResponseType e : ResourceResponse.ResponseType.values()) {
                    if (e.value.equals(value)) {
                        return e;
                    }
                }

                throw new IllegalArgumentException("Unexpected value '" + value + "'");
            }
        }

        @JsonProperty(value = "type", required = true)
        private ResponseType type;
        @JsonProperty(value = "uri", required = true)
        private String uri;
        @JsonProperty(value = "mimeType", required = true)
        private String mimeType;
    }

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
                  "text":{
                     "connect.index":3,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  },
                  "blob":{
                     "connect.index":4,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
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
    public static class BlobResourceResponse extends ResourceResponse {
        @JsonProperty(value = "blob", required = true)
        private String blob;

        public BlobResourceResponse(String uri, String mimeType, String blob) {
            super(ResponseType.BLOB, uri, mimeType);
            this.blob = blob;
        }
    }

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
                  "text":{
                     "connect.index":3,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
                  },
                  "blob":{
                     "connect.index":4,
                     "oneOf":[
                        {
                           "type":"null"
                        },
                        {
                           "type":"string"
                        }
                     ]
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
    public static class TextResourceResponse extends ResourceResponse {
        @JsonProperty(value = "text", required = true)
        private String text;

        public TextResourceResponse(String uri, String mimeType, String text) {
            super(ResponseType.TEXT, uri, mimeType);
            this.text = text;
        }
    }

    @Schema(value = """
            {
               "properties":{
                  "uri":{
                     "connect.index":0,
                     "type":"string"
                  }
               },
               "required":[
                  "uri"
               ],
               "title":"Record",
               "type":"object"
            }""", refs = {})
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceRequest {
        @JsonProperty(value = "uri", required = true)
        private String uri;
    }

}
