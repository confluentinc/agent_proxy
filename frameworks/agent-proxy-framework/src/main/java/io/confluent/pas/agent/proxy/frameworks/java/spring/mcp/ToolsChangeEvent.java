package io.confluent.pas.agent.proxy.frameworks.java.spring.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Event class for notifying changes in MCP tools configuration.
 * Extends Spring's ApplicationEvent to integrate with Spring's event handling system.
 * Used to broadcast updates when the available tools list changes in the MCP system.
 */
@Getter
public class ToolsChangeEvent extends ApplicationEvent {

    /**
     * List of MCP tools that have been updated or changed.
     * Each tool contains its configuration and capabilities as defined in the MCP schema.
     */
    private final List<McpSchema.Tool> tools;

    /**
     * Constructs a new ToolsChangeEvent with the source object and updated tools list.
     *
     * @param source The object on which the event initially occurred
     * @param tools  List of updated MCP tools
     */
    public ToolsChangeEvent(Object source, List<McpSchema.Tool> tools) {
        super(source);
        this.tools = tools;
    }
}