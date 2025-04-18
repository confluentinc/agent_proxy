package io.confluent.pas.mcp.proxy.frameworks.client.exceptions;

/**
 * Exception thrown when there is an issue with the configuration.
 */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
