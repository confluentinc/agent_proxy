package io.confluent.pas.agent.proxy.frameworks.java.spring.mcp;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AsyncMcpToolCallbackProviderTest {

    @Mock
    private McpAsyncClient mcpClient;

    private AsyncMcpToolCallbackProvider provider;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        provider = new AsyncMcpToolCallbackProvider(List.of(mcpClient));
    }

    @Test
    public void testGetToolCallbacks() {
        McpSchema.Tool tool1 = new McpSchema.Tool("allowedTool", "description", "{}");
        McpSchema.Tool tool2 = new McpSchema.Tool("deniedTool", "description", "{}");

        when(mcpClient.listTools()).thenReturn(Mono.just(new McpSchema.ListToolsResult(List.of(tool1, tool2), "")));

        provider.allow("allowedTool");
        provider.deny("deniedTool");

        ToolCallback[] toolCallbacks = provider.getToolCallbacks();

        assertEquals(1, toolCallbacks.length);
        assertEquals("allowedTool", toolCallbacks[0].getToolDefinition().name());
    }

    @Test
    public void testGetToolCallbacksNoTools() {
        when(mcpClient.listTools()).thenReturn(Mono.just(new McpSchema.ListToolsResult(List.of(), "")));

        ToolCallback[] toolCallbacks = provider.getToolCallbacks();

        assertEquals(0, toolCallbacks.length);
    }

    @Test
    public void testGetToolCallbacksNullResponse() {
        when(mcpClient.listTools()).thenReturn(Mono.empty());

        ToolCallback[] toolCallbacks = provider.getToolCallbacks();

        assertEquals(0, toolCallbacks.length);
    }
}