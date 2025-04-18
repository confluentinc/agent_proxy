package io.confluent.pas.mcp.proxy.frameworks.client;

/**
 * The `Agent` class is responsible for managing communication between MCP (Model Context Protocol)
 * and Kafka. It initializes the MCP client using Stdio transport, handles subscriptions to Kafka topics,
 * and processes requests from Python-based tools.
 * <p>
 * Key Responsibilities:
 * - Initializes and configures the MCP client.
 * - Manages Kafka topic subscriptions via SubscriptionHandlers.
 * - Handles incoming requests and responses using AgentRequestHandler.
 * - Ensures proper cleanup of resources upon shutdown.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.mcp.proxy.frameworks.client.internal.*;
import io.confluent.pas.agent.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.mcp.proxy.frameworks.client.exceptions.AgentException;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class Agent {
    // Asynchronous client for handling communication with MCP.
    private final McpAsyncClient mcpAsyncClient;
    // Configuration containing agent and mcpTool settings.
    private final AgentConfiguration agentConfiguration;
    // Configuration settings for managing Kafka connectivity and topics.
    private final KafkaConfiguration kafkaConfiguration;

    // List of handlers managing subscriptions to Kafka topics.
    private final List<SubscriptionHandler<Key, JsonNode, JsonNode>> handlers = new ArrayList<>();

    /**
     * Constructs an Agent instance and initializes the MCP client with Stdio transport.
     *
     * @param kafkaConfiguration Configuration for Kafka connectivity.
     * @param agentConfiguration Configuration containing agent settings and mcpTool configurations.
     */
    public Agent(KafkaConfiguration kafkaConfiguration, AgentConfiguration agentConfiguration) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.agentConfiguration = agentConfiguration;

        var transport = new StdioClientTransport(ServerParameters.builder(agentConfiguration.getCommand())
                .args(agentConfiguration.getArguments())
                .env(System.getenv())
                .build());

        this.mcpAsyncClient = McpClient.async(transport)
                .clientInfo(new McpSchema.Implementation("mcp-client-proxy", "0.1.0"))
                .capabilities(McpSchema.ClientCapabilities.builder().roots(true).build())
                .build();
    }

    /**
     * Initializes the agent after construction.
     * Starts the MCP client and sets up mcpTool subscriptions.
     */
    @PostConstruct
    public void init() {
        log.info("Initializing agent");
        mcpAsyncClient.initialize().block();

        new AgentInitializer(mcpAsyncClient, agentConfiguration)
                .initialize()
                .doOnSuccess(this::setupSubscriptionHandler)
                .subscribe();
    }

    /**
     * Cleans up resources when the agent shuts down.
     * Ensures all subscription handlers are properly stopped.
     */
    @PreDestroy
    public void destroy() {
        handlers.forEach(SubscriptionHandler::close);
        handlers.clear();
    }

    /**
     * Configures subscription handlers for MCP mcpTools.
     * Registers the mcpTool, associates request handling logic, and subscribes to Kafka topics.
     *
     * @param agentToolHandlers List of tool handlers for managing requests.
     */
    private void setupSubscriptionHandler(List<AgentToolHandler> agentToolHandlers) {
        agentToolHandlers.forEach(handler -> {
            final Schemas.Registration registration = new Schemas.Registration(
                    handler.mcpTool().name(),
                    handler.mcpTool().description(),
                    handler.tool().getRequest_topic(),
                    handler.tool().getResponse_topic());

            final SubscriptionHandler<Key, JsonNode, JsonNode> subscriptionHandler = new SubscriptionHandler<>(
                    new KafkaToolConfiguration(kafkaConfiguration, handler.tool()),
                    Key.class,
                    JsonNode.class,
                    JsonNode.class);

            final AgentRequestHandler requestHandler = new AgentRequestHandler(
                    mcpAsyncClient,
                    handler.tool());

            subscriptionHandler.subscribeWith(
                    registration,
                    createInputSchema(handler.mcpTool()),
                    handler.tool().getOutput_schema(),
                    requestHandler::handleRequest);

            handlers.add(subscriptionHandler);
        });
    }

    /**
     * Generates a JSON schema for the mcpTool's expected input parameters.
     *
     * @param tool The MCP tool whose schema is to be created.
     * @return A JsonSchema representing the tool's input structure.
     * @throws AgentException if an error occurs while serializing the schema.
     */
    private JsonSchema createInputSchema(McpSchema.Tool tool) {
        try {
            return new JsonSchema(JsonUtils.toString(tool.inputSchema()));
        } catch (JsonProcessingException e) {
            throw new AgentException("Error serializing input schema.", e);
        }
    }
}