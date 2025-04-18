package io.confluent.pas.mcp.demo.mcp;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Represents a connection to an MCP server.
 */
public interface McpServerConnection {

    /**
     * Execute a tool on the server.
     *
     * @param request The request to execute the tool.
     * @return The result of the tool execution.
     */
    String executeTool(ToolExecutionRequest request);

    /**
     * Get the name of the server.
     *
     * @return The name of the server.
     */
    String getServerName();

    /**
     * Get the version of the server.
     *
     * @return The version of the server.
     */
    String getServerVersion();

    /**
     * Check if the connection is established.
     *
     * @return true if the connection is established, false otherwise.
     */
    boolean isConnected();

    /**
     * Connect to the server.
     *
     * @return A {@link Mono} that completes when the connection is established.
     */
    Mono<Void> connect();

    /**
     * Check if the server supports tools.
     *
     * @return true if the server supports tools, false otherwise.
     */
    boolean isToolSupported();

    /**
     * Get the list of tools supported by the server.
     *
     * @return The list of tools supported by the server.
     */
    List<McpSchema.Tool> getTools();

}
