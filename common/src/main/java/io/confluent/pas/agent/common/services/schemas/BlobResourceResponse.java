package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlobResourceResponse extends ResourceResponse {
    private final static String BLOB = "blob";

    public String getBlob() {
        return getMetaDataValue(BLOB, String.class);
    }

    public void setBlob(String blob) {
        setMetaDataValue(BLOB, blob);
    }

    public BlobResourceResponse(String uri, String mimeType, String blob) {
        super(ResourceResponse.BLOB_TYPE, uri, mimeType);

        setBlob(blob);
    }
}