package io.confluent.pas.agent.proxy.frameworks.java.spring.mcp;

import io.confluent.pas.agent.common.services.Schemas;
import io.modelcontextprotocol.client.McpAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.mcp.AsyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider that produces MCP tool callbacks for asynchronous MCP clients.
 * Implements Spring AI's ToolCallbackProvider to integrate asynchronous MCP tools
 * into the Spring AI ecosystem.
 */
@Slf4j
public class AsyncMcpToolCallbackProvider extends McpToolFilters<AsyncMcpToolCallbackProvider> implements ToolCallbackProvider {

    /**
     * List of asynchronous MCP clients to fetch tools from
     */
    private final List<McpAsyncClient> clients;

    /**
     * Constructs a provider with a registration and a list of asynchronous MCP clients.
     *
     * @param registration Tool registration information
     * @param clients      List of asynchronous MCP clients
     */
    public AsyncMcpToolCallbackProvider(Schemas.Registration registration, List<McpAsyncClient> clients) {
        super(registration);
        this.clients = clients;
    }

    /**
     * Convenience constructor accepting variable arguments of asynchronous MCP clients.
     *
     * @param registration Tool registration information
     * @param clients      Variable number of asynchronous MCP clients
     */
    public AsyncMcpToolCallbackProvider(Schemas.Registration registration, McpAsyncClient... clients) {
        super(registration);
        this.clients = List.of(clients);
    }

    /**
     * Constructs a provider with a registration and a list of asynchronous MCP clients.
     *
     * @param clients List of asynchronous MCP clients
     */
    public AsyncMcpToolCallbackProvider(List<McpAsyncClient> clients) {
        this.clients = clients;
    }

    /**
     * Convenience constructor accepting variable arguments of asynchronous MCP clients.
     *
     * @param clients Variable number of asynchronous MCP clients
     */
    public AsyncMcpToolCallbackProvider(McpAsyncClient... clients) {
        this.clients = List.of(clients);
    }

    /**
     * Retrieves tool callbacks from all registered asynchronous MCP clients.
     * Filters out self-referential tools based on registration name.
     * Uses reactive programming with blocking calls to retrieve the data.
     *
     * @return Array of tool callbacks from all asynchronous MCP clients
     */
    @NotNull
    @Override
    public ToolCallback[] getToolCallbacks() {
        // Initialize collection to hold all tool callbacks
        List<AsyncMcpToolCallback> toolCallbackList = new ArrayList<>();

        for (McpAsyncClient mcpClient : this.clients) {
            // Retrieve tools from the client asynchronously, then:
            // 1. Filter out self-reference tools
            // 2. Convert tools to AsyncMcpToolCallback objects
            // 3. Block to get the results synchronously
            final List<AsyncMcpToolCallback> toolCallbacks = mcpClient.listTools()
                    .map((response) -> response
                            .tools()
                            .stream()
                            .filter(this::isToolAllowed)
                            .map((tool) -> new AsyncMcpToolCallback(mcpClient, tool))
                            .toList())
                    .block();

            // Log and skip if no tools were found or if the result is null
            if (toolCallbacks == null || toolCallbacks.isEmpty()) {
                log.warn("No tools found in MCP client.");
                continue;
            }

            // Add all found tools to the collection
            toolCallbackList.addAll(toolCallbacks);
        }

        // Convert list to array as required by the interface
        return toolCallbackList.toArray(new ToolCallback[0]);
    }
}