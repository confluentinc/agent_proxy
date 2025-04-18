package io.confluent.pas.agent.proxy.frameworks.java.spring.autoconfig;

import io.confluent.pas.agent.proxy.frameworks.java.spring.mcp.AsyncMcpToolCallbackProvider;
import io.confluent.pas.agent.proxy.frameworks.java.spring.mcp.ResourcesChangeEvent;
import io.confluent.pas.agent.proxy.frameworks.java.spring.mcp.ToolsChangeEvent;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Autoconfiguration class for Model Control Protocol (MCP) client.
 * This class is activated when the 'mcp.client.connection-string' property is present.
 */
@Slf4j
@AutoConfiguration
@AutoConfigureOrder
@ConditionalOnProperty(prefix = "mcp.client", name = "connection-string")
public class McpClientAutoConfiguration {

    /**
     * List of connection strings for the MCP client
     */
    @Value("#{'${mcp.client.connection-string}'.split(',')}")
    private List<String> connectionString;

    /**
     * Client request timeout in seconds, defaults to 60
     */
    @Value("${mcp.client.timeout:60}")
    private int timeout;

    /**
     * API key for authentication (optional)
     */
    @Value("${mcp.client.api.key:#{null}}")
    private String apiKey;

    /**
     * API secret for authentication (optional)
     */
    @Value("${mcp.client.api.secret:#{null}}")
    private String apiSecret;

    /**
     * Comma-separated list of tool names to be denied access
     */
    @Value("${mcp.client.deny-tools:#{null}}")
    private String deniedTools;

    /**
     * Creates a ToolCallbackProvider bean for handling MCP tool interactions.
     * Configures tool access restrictions if deny-tools are specified.
     *
     * @param mcpAsyncClient Async MCP client instance
     * @return Configured ToolCallbackProvider instance
     */
    @Bean
    @ConditionalOnMissingBean()
    public AsyncMcpToolCallbackProvider getToolCallbackProvider(McpAsyncClient mcpAsyncClient) {
        final AsyncMcpToolCallbackProvider provider = new AsyncMcpToolCallbackProvider(mcpAsyncClient);

        if (!StringUtils.isEmpty(deniedTools)) {
            final List<String> tools = List.of(deniedTools.split(","));
            return provider.denies(tools);
        }

        return provider;
    }

    /**
     * Creates a Server-Sent Events (SSE) transport bean for MCP client.
     * Only created when mcp.client.mode=sse.
     * Supports optional basic authentication using API key and secret.
     *
     * @return A configured WebFlux SSE transport instance
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.client", name = "mode", havingValue = "sse")
    public McpClientTransport sseServerTransport() {
        final WebClient.Builder webClientBuilder = (StringUtils.isNotEmpty(apiKey) && StringUtils.isNotEmpty(apiSecret))
                ? WebClient.builder().filter(ExchangeFilterFunctions.basicAuthentication(apiKey, apiSecret)).baseUrl(connectionString.getFirst())
                : WebClient.builder().baseUrl(connectionString.getFirst());

        return new WebFluxSseClientTransport(webClientBuilder);
    }

    /**
     * Creates a Standard IO transport bean for MCP client.
     * Only created when mcp.client.mode=stdio.
     * Supports command and arguments from connection string.
     *
     * @return A configured StdIO transport instance
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.client", name = "mode", havingValue = "stdio")
    public McpClientTransport stdioServerTransport() {
        final String command = connectionString.getFirst();
        final List<String> args = connectionString.size() > 1
                ? connectionString.stream().skip(1).collect(Collectors.toList())
                : null;

        ServerParameters params = ServerParameters.builder(command)
                .args(args)
                .build();

        return new StdioClientTransport(params);
    }

    /**
     * Creates and initializes the MCP async client bean.
     * Configures the client with timeout, capabilities, and event handlers for tools and resources changes.
     *
     * @param sseServerTransport        Transport layer for the client
     * @param applicationEventPublisher Spring event publisher for broadcasting changes
     * @return An initialized MCP async client
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.client", name = "mode")
    @ConditionalOnMissingBean()
    public McpAsyncClient getMcpAsyncClient(McpClientTransport sseServerTransport,
                                            ApplicationEventPublisher applicationEventPublisher) {
        final McpAsyncClient client = McpClient.async(sseServerTransport)
                .requestTimeout(Duration.ofSeconds(timeout))
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .roots(true)
                        .build())
                .toolsChangeConsumer(tools -> Mono.fromRunnable(() -> {
                    log.trace("Tools updated: {}", tools);
                    applicationEventPublisher.publishEvent(new ToolsChangeEvent(this, tools));
                }))
                .resourcesChangeConsumer(resources -> Mono.fromRunnable(() -> {
                    log.trace("Resources updated: {}", resources);
                    applicationEventPublisher.publishEvent(new ResourcesChangeEvent(this, resources));
                }))
                .build();

        client.initialize().block();

        return client;
    }

    @ConditionalOnMissingBean
    @Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        SimpleApplicationEventMulticaster eventMulticaster = new SimpleApplicationEventMulticaster();

        eventMulticaster.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return eventMulticaster;
    }

}