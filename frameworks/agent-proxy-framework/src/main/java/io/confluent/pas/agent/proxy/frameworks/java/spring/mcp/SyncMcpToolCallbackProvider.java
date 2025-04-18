package io.confluent.pas.agent.proxy.frameworks.java.spring.mcp;

import io.confluent.pas.agent.common.services.Schemas;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider that produces MCP tool callbacks for synchronous MCP clients.
 * Implements Spring AI's ToolCallbackProvider to integrate MCP tools into the Spring AI ecosystem.
 */
@Slf4j
public class SyncMcpToolCallbackProvider extends McpToolFilters<SyncMcpToolCallbackProvider> implements ToolCallbackProvider {

    /**
     * List of synchronous MCP clients to fetch tools from
     */
    private final List<McpSyncClient> clients;

    /**
     * Constructs a provider with a registration and a list of synchronous MCP clients.
     *
     * @param registration Tool registration information
     * @param clients      List of synchronous MCP clients
     */
    public SyncMcpToolCallbackProvider(Schemas.Registration registration, List<McpSyncClient> clients) {
        super(registration);
        this.clients = clients;
    }

    /**
     * Convenience constructor accepting variable arguments of MCP clients.
     *
     * @param registration Tool registration information
     * @param clients      Variable number of synchronous MCP clients
     */
    public SyncMcpToolCallbackProvider(Schemas.Registration registration, McpSyncClient... clients) {
        super(registration);
        this.clients = List.of(clients);
    }

    /**
     * Retrieves tool callbacks from all registered MCP clients.
     * Filters out self-referential tools based on registration name.
     *
     * @return Array of tool callbacks from all MCP clients
     */
    @NotNull
    @Override
    public ToolCallback[] getToolCallbacks() {
        List<SyncMcpToolCallback> toolCallbackList = new ArrayList<>();

        for (McpSyncClient mcpClient : this.clients) {
            // Retrieve tools from the client, filter out self-reference, and convert to callbacks
            final List<SyncMcpToolCallback> toolCallbacks = mcpClient.listTools()
                    .tools()
                    .stream()
                    .filter(this::isToolAllowed)
                    .map((tool) -> new SyncMcpToolCallback(mcpClient, tool))
                    .toList();

            // Log and skip if no tools were found
            if (toolCallbacks.isEmpty()) {
                log.warn("No tools found in MCP client.");
                continue;
            }

            toolCallbackList.addAll(toolCallbacks);
        }

        // Convert list to array as required by the interface
        return toolCallbackList.toArray(new ToolCallback[0]);
    }
}