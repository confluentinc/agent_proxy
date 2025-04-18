package io.confluent.pas.mcp.proxy.frameworks.client.internal;

import io.confluent.pas.mcp.proxy.frameworks.client.exceptions.AgentException;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
/**
 * The `AgentInitializer` class is responsible for initializing and validating MCP (Model Context Protocol) tools.
 * It retrieves available tools from the MCP client, verifies their configurations, and ensures proper retry mechanisms
 * for robustness. This process ensures that only correctly configured tools are registered for use.
 *
 * Key Responsibilities:
 * - Lists available MCP tools via asynchronous communication.
 * - Validates tools against the agent configuration.
 * - Implements a retry mechanism to handle transient failures.
 */
public class AgentInitializer {
    // Asynchronous client for retrieving available MCP tools and managing communication.
    private final McpAsyncClient mcpAsyncClient;
    // Configuration object containing MCP tool specifications and settings.
    private final AgentConfiguration config;

    /**
     * Constructs an `AgentInitializer` instance.
     *
     * @param mcpAsyncClient The async client for communicating with MCP.
     * @param config         Configuration object containing MCP tool specifications.
     */
    public AgentInitializer(McpAsyncClient mcpAsyncClient, AgentConfiguration config) {
        this.mcpAsyncClient = mcpAsyncClient;
        this.config = config;
    }

    /**
     * Initializes the MCP tool handling system.
     * This method retrieves a list of available tools from the MCP client,
     * validates them against the configuration, and applies a retry strategy
     * for resilience.
     *
     * @return A Mono containing the validated list of MCP tool handlers.
     * @throws AgentException if tool validation fails after retries.
     */
    public Mono<List<AgentToolHandler>> initialize() {
        return initialize(60, 1);
    }

    /**
     * Initializes the MCP tool handling system.
     * This method retrieves a list of available tools from the MCP client,
     * validates them against the configuration, and applies a retry strategy
     * for resilience.
     *
     * @param retryAttempts The number of retry attempts.
     * @param retryDelay    The delay between retry attempts.
     * @return A Mono containing the validated list of MCP tool handlers.
     * @throws AgentException if tool validation fails after retries.
     */
    public Mono<List<AgentToolHandler>> initialize(int retryAttempts, int retryDelay) {
        return mcpAsyncClient.listTools()
                .flatMap(tools -> findAndValidateTool(tools.tools()))
                .retryWhen(Retry.fixedDelay(retryAttempts, Duration.ofSeconds(retryDelay))
                        .filter(error -> error instanceof AgentException)
                        .doBeforeRetry(retrySignal ->
                                log.warn("Retrying initialization attempt {}", retrySignal.totalRetries()))
                )
                .doOnSuccess(result -> log.info("Agent initialization completed"))
                .doOnError(err -> log.error("Error initializing agent", err));
    }

    /**
     * Finds and validates MCP tools from the provided list by matching them
     * against the configured specifications.
     *
     * @param tools List of available MCP tools retrieved from the MCP client.
     * @return A Mono containing the validated list of MCP tool handlers.
     * @throws AgentException if no matching tools are found.
     */
    Mono<List<AgentToolHandler>> findAndValidateTool(List<McpSchema.Tool> tools) {
        if (tools.isEmpty()) {
            return Mono.error(new AgentException("No tools found"));
        }

        final Map<String, AgentConfiguration.ToolConfiguration> allTools = config.getTools()
                .stream()
                .collect(Collectors.toMap(AgentConfiguration.ToolConfiguration::getName, Function.identity()));

        return Mono.just(tools.stream()
                .filter(t -> allTools.containsKey(t.name()))
                .map(tool -> new AgentToolHandler(tool, allTools.get(tool.name())))
                .toList());
    }
}