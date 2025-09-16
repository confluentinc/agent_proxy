package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TextResourceResponse extends ResourceResponse {
    private final static String TEXT = "text";

    public String getText() {
        return getMetaDataValue(TEXT, String.class);
    }

    public void setText(String text) {
        setMetaDataValue(TEXT, text);
    }

    public TextResourceResponse(String uri, String mimeType, String text) {
        super(ResourceResponse.TEXT_TYPE, uri, mimeType, new HashMap<>());

        setText(text);
    }
}