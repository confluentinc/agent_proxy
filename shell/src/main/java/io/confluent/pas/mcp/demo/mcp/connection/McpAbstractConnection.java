package io.confluent.pas.mcp.demo.mcp.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.mcp.demo.mcp.McpServerConnection;
import io.confluent.pas.mcp.demo.mcp.tools.ToolExecutionHelper;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a connection to an MCP server.
 */
@Getter
public abstract class McpAbstractConnection implements McpServerConnection {
    private McpAsyncClient client;
    private List<McpSchema.Tool> tools;
    private McpSchema.InitializeResult initializeResult;

    public boolean isConnected() {
        return client != null;
    }

    public boolean isToolSupported() {
        return tools != null && !tools.isEmpty();
    }

    public String getServerName() {
        return initializeResult.serverInfo().name();
    }

    public String getServerVersion() {
        return initializeResult.serverInfo().version();
    }

    public String executeTool(ToolExecutionRequest request) {
        final Map<String, Object> arguments;
        try {
            arguments = JsonUtils.toMap(request.arguments());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        final McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest(
                request.name(),
                arguments
        );

        AtomicReference<String> result = new AtomicReference<>();

        client.callTool(callToolRequest)
                .doOnSuccess(response -> {
                    try {
                        result.set(ToolExecutionHelper.extractResult(response));
                    } catch (JsonProcessingException e) {
                        result.set(e.getMessage());
                    }
                })
                .doOnError(throwable -> {
                    result.set(throwable.getMessage());
                })
                .block();

        return result.get();
    }

    @Override
    public Mono<Void> connect() {
        final McpClientTransport transport = getTransport();

        return Mono.create(sink -> {
            client = McpClient.async(transport)
                    .requestTimeout(Duration.ofSeconds(10))
                    .capabilities(McpSchema.ClientCapabilities
                            .builder()
                            .build())
                    .toolsChangeConsumer(tools -> Mono.fromRunnable(() -> {
                        this.tools = tools;
                    }))
                    .build();

            initialize(sink);
        });
    }

    /**
     * Initialize the connection.
     *
     * @param sink the sink to complete when the connection is initialized
     */
    protected void initialize(MonoSink<Void> sink) {
        client.initialize()
                .doOnSuccess(v -> {
                    initializeResult = v;
                    sink.success();
                })
                .doOnError(sink::error)
                .block();

        tools = Objects.requireNonNull(client.listTools().block()).tools();
    }

    /**
     * Get the transport for this connection.
     *
     * @return the transport
     */
    protected abstract McpClientTransport getTransport();

}
