package io.confluent.pas.agent.proxy.frameworks.java.spring.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Event class for notifying changes in MCP resources configuration.
 * Extends Spring's ApplicationEvent to integrate with Spring's event handling system.
 * Used to broadcast updates when the available resources list changes in the MCP system.
 */
@Getter
public class ResourcesChangeEvent extends ApplicationEvent {

    /**
     * List of MCP resources that have been updated or changed.
     * Each resource contains its configuration and state as defined in the MCP schema.
     */
    private final List<McpSchema.Resource> resources;

    /**
     * Constructs a new ResourcesChangeEvent with the source object and updated resources list.
     *
     * @param source The object on which the event initially occurred
     * @param tools  List of updated MCP resources
     */
    public ResourcesChangeEvent(Object source, List<McpSchema.Resource> tools) {
        super(source);
        this.resources = tools;
    }
}