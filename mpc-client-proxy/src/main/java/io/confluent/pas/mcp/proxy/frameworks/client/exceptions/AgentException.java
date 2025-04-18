package io.confluent.pas.mcp.proxy.frameworks.client.exceptions;

/**
 * Exception thrown when there is an issue with the agent.
 */
public class AgentException extends RuntimeException {
    public AgentException(String message) {
        super(message);
    }

    public AgentException(Throwable cause) {
        super(cause);
    }

    public AgentException(String message, Throwable cause) {
        super(message, cause);
    }
}

