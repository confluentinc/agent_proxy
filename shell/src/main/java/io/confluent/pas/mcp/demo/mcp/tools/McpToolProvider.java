package io.confluent.pas.mcp.demo.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import io.confluent.pas.mcp.demo.mcp.McpServerConnection;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ToolProvider} that provides tools from an MCP server.
 */
public class McpToolProvider implements ToolProvider {

    private final List<McpServerConnection> connections;

    public McpToolProvider(List<McpServerConnection> connections) {
        this.connections = connections;
    }

    @Override
    public ToolProviderResult provideTools(ToolProviderRequest toolProviderRequest) {
        final Map<ToolSpecification, ToolExecutor> tools = new HashMap<>();

        connections.forEach(connection -> {
            final List<McpSchema.Tool> mcpTools = connection.getTools();
            mcpTools.forEach(tool -> {
                final ToolSpecification spec;
                try {
                    spec = ToolSpecificationHelper.fromMcpTool(tool);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to parse tool specification", e);
                }

                tools.put(spec, (executionRequest, memoryId) -> connection.executeTool(executionRequest));
            });

        });

        return new ToolProviderResult(tools);
    }
}
