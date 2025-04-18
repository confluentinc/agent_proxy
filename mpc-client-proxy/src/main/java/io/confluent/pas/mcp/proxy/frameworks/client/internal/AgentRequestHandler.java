package io.confluent.pas.mcp.proxy.frameworks.client.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.frameworks.java.Request;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.mcp.proxy.frameworks.client.exceptions.AgentException;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Handles requests and responses between MCP tools and the agent system.
 * This class is responsible for processing mcpTool requests, managing responses,
 * and handling data transformations between different formats.
 */
@Slf4j
public class AgentRequestHandler {

    /**
     * Client for asynchronous MCP communication
     */
    private final McpAsyncClient mcpAsyncClient;
    /**
     * Configuration containing mcpTool settings
     */
    private final AgentConfiguration.ToolConfiguration tool;
    /**
     * Deserializer for converting JSON responses to the expected format
     */
    private final JsonResponseDeserializer deserializer;

    /**
     * Constructs an AgentRequestHandler instance.
     *
     * @param mcpAsyncClient The async client for communicating with MCP
     * @param tool           Configuration object containing mcpTool specifications
     */
    public AgentRequestHandler(McpAsyncClient mcpAsyncClient, AgentConfiguration.ToolConfiguration tool) {
        this.mcpAsyncClient = mcpAsyncClient;
        this.tool = tool;
        this.deserializer = new JsonResponseDeserializer(tool);
    }

    /**
     * Processes incoming mcpTool requests by calling the appropriate MCP mcpTool
     * and handling the response.
     *
     * @param request The incoming request containing key, generic request data, and JSON payload
     */
    public void handleRequest(Request<Key, JsonNode, JsonNode> request) {
        final Map<String, Object> genericRequest = JsonUtils.toMap(request.getRequest());

        mcpAsyncClient.callTool(new McpSchema.CallToolRequest(tool.getName(), genericRequest))
                .flatMap(result -> processToolResponse(result, request))
                .doOnError(error -> log.error("Error processing request", error))
                .block();
    }

    /**
     * Processes the mcpTool's response by converting it to the expected format
     * and sending it back through the request channel.
     *
     * @param result  The result from the MCP mcpTool call
     * @param request The original request for context
     * @return A Mono completing when the response is processed
     * @throws AgentException if response processing fails or unexpected response type is received
     */
    private Mono<Void> processToolResponse(McpSchema.CallToolResult result, Request<Key, JsonNode, JsonNode> request) {
        if (!(result.content().getFirst() instanceof McpSchema.TextContent textContent)) {
            return Mono.error(new AgentException("Unexpected response type"));
        }

        return request.respond(deserializer.deserialize(textContent.text()));
    }
}