package io.confluent.pas.mcp.proxy.frameworks.client.internal;

import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.mcp.proxy.frameworks.client.exceptions.AgentException;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;


public class AgentInitializerTest {

    @Mock
    private McpAsyncClient mcpAsyncClient;

    @Mock
    private AgentConfiguration config;

    private AgentInitializer agentInitializer;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        agentInitializer = new AgentInitializer(mcpAsyncClient, config);
    }

    @Test
    public void testInitializeSuccess() {
        McpSchema.Tool tool = new McpSchema.Tool("testTool", "description", "{}");
        AgentConfiguration.ToolConfiguration toolConfig = new AgentConfiguration.ToolConfiguration(
                "testTool",
                new JsonSchema("{}"),
                "requestTopic",
                "responseTopic");

        when(mcpAsyncClient.listTools()).thenReturn(Mono.just(new McpSchema.ListToolsResult(List.of(tool), "")));
        when(config.getTools()).thenReturn(List.of(toolConfig));

        StepVerifier.create(agentInitializer.initialize())
                .expectNextMatches(agentToolHandlers -> agentToolHandlers.size() == 1 && agentToolHandlers.get(0).mcpTool().name().equals("testTool"))
                .verifyComplete();
    }

    @Test
    public void testInitializeNoTools() {
        when(mcpAsyncClient.listTools()).thenReturn(Mono.just(new McpSchema.ListToolsResult(List.of(), "")));

        StepVerifier.create(agentInitializer.initialize(2, 1))
                .expectErrorMatches(throwable -> throwable.getCause() instanceof AgentException && throwable.getCause().getMessage().equals("No tools found"))
                .verify();
    }

    @Test
    public void testInitializeRetry() {
        when(mcpAsyncClient.listTools()).thenReturn(Mono.error(new AgentException("Initialization failed")));

        StepVerifier.create(agentInitializer.initialize(2, 1))
                .expectErrorMatches(throwable -> throwable.getCause() instanceof AgentException && throwable.getCause().getMessage().equals("Initialization failed"))
                .verify();
    }

    @Test
    public void testFindAndValidateToolSuccess() {
        McpSchema.Tool tool = new McpSchema.Tool("testTool", "description", "{}");
        AgentConfiguration.ToolConfiguration toolConfig = new AgentConfiguration.ToolConfiguration(
                "testTool",
                new JsonSchema("{}"),
                "requestTopic",
                "responseTopic");

        when(config.getTools()).thenReturn(List.of(toolConfig));

        StepVerifier.create(agentInitializer.findAndValidateTool(List.of(tool)))
                .expectNextMatches(agentToolHandlers -> agentToolHandlers.size() == 1 && agentToolHandlers.get(0).mcpTool().name().equals("testTool"))
                .verifyComplete();
    }

    @Test
    public void testFindAndValidateToolNoMatchingTools() {
        McpSchema.Tool tool = new McpSchema.Tool("testTool", "description", "{}");

        when(config.getTools()).thenReturn(List.of());

        StepVerifier.create(agentInitializer.findAndValidateTool(List.of(tool)))
                .expectNextMatches(agentToolHandlers -> agentToolHandlers.isEmpty())
                .verifyComplete();
    }
}