package io.confluent.pas.agent.common.services.schemas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.confluent.pas.agent.common.utils.UriUtils;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * Represents a resource registration in the system, extending the base Registration class.
 * This class manages resource-specific attributes such as URLs and MIME types.
 * <p>
 * Each resource registration contains:
 * - A URL identifying the resource location
 * - A MIME type specifying the resource format
 * - Metadata storage for additional resource properties
 * <p>
 * The class provides methods to manage these attributes while ensuring data validity
 * through input validation. It supports template URLs and follows RESTful principles
 * for resource identification.
 */
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceRegistration extends Registration {
    /**
     * Key used to store and retrieve the MIME type in the metadata map.
     * This constant ensures consistent access to MIME type information.
     */
    private final static String MIME_TYPE = "mimeType";

    /**
     * Key used to store and retrieve the URL in the metadata map.
     * This constant ensures consistent access to URL information.
     */
    private final static String URL = "url";

    /**
     * Retrieves the URL associated with this resource.
     *
     * @return The URL string stored in metadata
     */
    public String getUrl() {
        return getMetaDataValue(URL, String.class);
    }

    /**
     * Sets the URL for this resource after validation.
     *
     * @param url The URL to set
     * @throws IllegalArgumentException if the URL is blank
     */
    public void setUrl(String url) {
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("url cannot be blank");
        }

        setMetaDataValue(URL, url.startsWith("/") ? url.substring(1) : url);
    }

    /**
     * Retrieves the MIME type of this resource.
     *
     * @return The MIME type string stored in metadata
     */
    public String getMimeType() {
        return getMetaDataValue(MIME_TYPE, String.class);
    }

    /**
     * Sets the MIME type for this resource after validation.
     *
     * @param mimeType The MIME type to set
     * @throws IllegalArgumentException if the MIME type is blank
     */
    public void setMimeType(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            throw new IllegalArgumentException("mimeType cannot be blank");
        }

        setMetaDataValue(MIME_TYPE, mimeType);
    }

    /**
     * Checks if the resource URL is a template.
     *
     * @return true if the URL contains template parameters, false otherwise
     */
    @JsonIgnore
    public boolean isTemplate() {
        return UriUtils.isTemplate(getUrl());
    }

    /**
     * Creates a new ResourceRegistration with the specified parameters.
     *
     * @param name              The name of the resource
     * @param description       The description of the resource
     * @param requestTopicName  The name of the request topic
     * @param responseTopicName The name of the response topic
     * @param mimeType          The MIME type of the resource
     * @param url               The URL of the resource
     */
    public ResourceRegistration(String name,
                                String description,
                                String requestTopicName,
                                String responseTopicName,
                                String mimeType,
                                String url) {
        super(RESOURCE, name, description, requestTopicName, responseTopicName, "N/A", new HashMap<>());

        setMimeType(mimeType);
        setUrl(url);
    }
}

