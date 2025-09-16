package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public abstract class AbstractSchema {
    private Map<String, Object> metaData;

    @JsonProperty(value = "metaData")
    public Map<String, Object> getMetaData() {
        return metaData;
    }

    @JsonProperty(value = "metaData")
    public void setMetaData(Map<String, Object> metaData) {
        this.metaData = metaData;
    }
    
    protected <T> T getMetaDataValue(String key, Class<T> type) {
        if (metaData == null || !metaData.containsKey(key)) {
            return null;
        }

        Object value = metaData.get(key);
        if (value == null) {
            return null;
        }

        if (type.isInstance(value)) {
            return type.cast(value);
        }

        throw new IllegalArgumentException("Meta data value for key '" + key + "' is not of type " + type.getName());
    }

    protected void setMetaDataValue(String key, Object value) {
        if (metaData == null) {
            metaData = new HashMap<>();
        }

        metaData.put(key, value);
    }
}
