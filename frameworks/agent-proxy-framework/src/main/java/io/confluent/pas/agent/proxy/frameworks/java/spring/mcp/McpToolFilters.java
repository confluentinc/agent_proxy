package io.confluent.pas.agent.proxy.frameworks.java.spring.mcp;

import io.confluent.pas.agent.common.services.Schemas;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing a set of filters for allowed and denied tools.
 */
@Slf4j
@Getter
class McpToolFilters<T extends McpToolFilters<T>> {

    /**
     * List of denied tools.
     */
    private final List<String> deniedTools = new ArrayList<>();

    /**
     * List of allowed tools.
     */
    private final List<String> allowedTools = new ArrayList<>();

    /**
     * Constructor initializing the denied tools list with the registration name and an empty allowed tools list.
     *
     * @param registration Tool registration information.
     */
    public McpToolFilters(Schemas.Registration registration) {
        deny(registration.getName());
    }

    public McpToolFilters() {
    }

    /**
     * Checks if a tool is allowed.
     *
     * @param tool Tool to check.
     * @return True if the tool is in the allowed list and not in the denied list.
     */
    public boolean isToolAllowed(McpSchema.Tool tool) {
        return (allowedTools.isEmpty() || allowedTools.contains(tool.name())) && !deniedTools.contains(tool.name());
    }

    /**
     * Adds a tool to the denied list.
     *
     * @param toolName Tool to deny.
     * @return The current McpToolFilters instance.
     */
    @SuppressWarnings("unchecked")
    public T deny(String toolName) {
        if (StringUtils.isEmpty(toolName)) {
            log.warn("Attempted to deny an empty tool name.");
            return (T) this;
        }

        deniedTools.add(toolName);
        return (T) this;
    }

    /**
     * Adds multiple tools to the denied list.
     *
     * @param deniedTools List of tools to deny.
     * @return The current McpToolFilters instance.
     */
    @SuppressWarnings("unchecked")
    public T denies(List<String> deniedTools) {
        if (deniedTools == null || deniedTools.isEmpty()) {
            log.warn("Attempted to deny an empty list of tools.");
            return (T) this;
        }

        this.deniedTools.addAll(deniedTools);
        return (T) this;
    }

    /**
     * Adds a tool to the allowed list.
     *
     * @param toolName Tool to allow.
     * @return The current McpToolFilters instance.
     */
    @SuppressWarnings("unchecked")
    public T allow(String toolName) {
        if (StringUtils.isEmpty(toolName)) {
            log.warn("Attempted to allow an empty tool name.");
            return (T) this;
        }

        allowedTools.add(toolName);
        return (T) this;
    }

    /**
     * Adds multiple tools to the allowed list.
     *
     * @param allowedTools List of tools to allow.
     * @return The current McpToolFilters instance.
     */
    @SuppressWarnings("unchecked")
    public T allows(List<String> allowedTools) {
        if (allowedTools == null || allowedTools.isEmpty()) {
            log.warn("Attempted to allow an empty list of tools.");
            return (T) this;
        }

        this.allowedTools.addAll(allowedTools);
        return (T) this;
    }
}