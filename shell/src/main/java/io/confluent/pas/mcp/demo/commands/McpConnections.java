package io.confluent.pas.mcp.demo.commands;

import io.confluent.pas.mcp.demo.mcp.McpServerConnection;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Component
public class McpConnections {
    private final Map<String, McpServerConnection> mcpServers = new HashMap<>();

    public List<McpServerConnection> getConnections() {
        return List.copyOf(mcpServers.values());
    }

    public boolean isEmpty() {
        return mcpServers.isEmpty();
    }

    public boolean containsServer(String serverName) {
        return mcpServers.containsKey(serverName);
    }

    public void forEach(BiConsumer<String, McpServerConnection> action) {
        mcpServers.forEach(action);
    }

    public void addServer(final String key, McpServerConnection connection) {
        mcpServers.put(key, connection);
    }

    public McpServerConnection getServer(String serverName) {
        return mcpServers.get(serverName);
    }
}
