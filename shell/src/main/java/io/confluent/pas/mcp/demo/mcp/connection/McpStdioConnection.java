package io.confluent.pas.mcp.demo.mcp.connection;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Represents a connection to an MCP server.
 */
@Getter
@AllArgsConstructor
public class McpStdioConnection extends McpAbstractConnection {

    private String command;
    private List<String> args;

    @Override
    protected McpClientTransport getTransport() {
        ServerParameters params = ServerParameters.builder(command)
                .args(args)
                .build();

        return new StdioClientTransport(params);
    }
}
