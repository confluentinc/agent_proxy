package io.confluent.pas.mcp.proxy.frameworks.client.internal;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Handles mcpTool-related operations for the MCP (Model Context Protocol) agent.
 * This record encapsulates a mcpTool's metadata and provides matching functionality.
 *
 * @param mcpTool The MCP mcpTool definition containing the mcpTool's specifications
 * @param tool    The name identifier for the mcpTool
 */
public record AgentToolHandler(McpSchema.Tool mcpTool, AgentConfiguration.ToolConfiguration tool) {
    /**
     * Checks if the mcpTool matches the given name.
     *
     * @param name The name to match against the mcpTool's name
     * @return true if the mcpTool exists and its name matches the given name, false otherwise
     */
    public boolean matches(String name) {
        return mcpTool != null && mcpTool.name().equals(name);
    }
}