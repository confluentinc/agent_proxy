package io.confluent.pas.agent.proxy.registration.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ToolHandlerTest {

    @Mock
    private Schemas.Registration registration;

    @Mock
    private SchemaRegistryClient schemaRegistryClient;

    @Mock
    private RequestResponseHandler requestResponseHandler;

    @Mock
    private McpAsyncServer mcpServer;

    private ToolHandler toolHandler;

    @BeforeEach
    void setUp() throws RestClientException, IOException {
        MockitoAnnotations.openMocks(this);

        when(schemaRegistryClient.getLatestSchemaMetadata(any())).thenReturn(new SchemaMetadata(
                1,
                1,
                "{\"type\":\"string\"}"
        ));

        toolHandler = new ToolHandler(registration, schemaRegistryClient, requestResponseHandler);
    }

    @Test
    void testRegister() {
        when(registration.getName()).thenReturn("test-tool");
        when(registration.getDescription()).thenReturn("Test Tool");

        McpSchema.Tool tool = new McpSchema.Tool("test-tool", "Test Tool", "null");
        McpServerFeatures.AsyncToolSpecification toolRegistration = new McpServerFeatures.AsyncToolSpecification(tool, null);

        when(mcpServer.addTool(any(McpServerFeatures.AsyncToolSpecification.class)))
                .thenReturn(Mono.empty());

        Mono<Void> result = toolHandler.register(mcpServer);
        assertNotNull(result);
        verify(mcpServer).addTool(any(McpServerFeatures.AsyncToolSpecification.class));
    }

    @Test
    void testUnregister() {
        when(registration.getName()).thenReturn("test-tool");

        when(mcpServer.removeTool("test-tool")).thenReturn(Mono.empty());

        Mono<Void> result = toolHandler.unregister(mcpServer);
        assertNotNull(result);
        verify(mcpServer).removeTool("test-tool");
    }

    @Test
    void testSendRequest() throws ExecutionException, InterruptedException {
        Map<String, Object> arguments = Map.of("key", "value");

        when(requestResponseHandler.sendRequestResponse(any(), any(), anyString(), anyMap()))
                .thenReturn(Mono.just(mock(JsonNode.class)));

        Mono<JsonNode> result = toolHandler.sendRequest(arguments);
        assertNotNull(result);
        verify(requestResponseHandler).sendRequestResponse(any(), any(), anyString(), anyMap());
    }

    @Test
    void testSendToolRequest() throws ExecutionException, InterruptedException {
        Map<String, Object> arguments = Map.of("key", "value");
        MonoSink<McpSchema.CallToolResult> sink = mock(MonoSink.class);

        when(requestResponseHandler.sendRequestResponse(any(), any(), anyString(), anyMap()))
                .thenReturn(Mono.just(mock(JsonNode.class)));

        toolHandler.sendToolRequest(arguments, sink);
        verify(requestResponseHandler).sendRequestResponse(any(), any(), anyString(), anyMap());
    }
}