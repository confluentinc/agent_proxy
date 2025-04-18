package io.confluent.pas.agent.proxy.frameworks.java.spring.autoconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * Auto-configuration class for Model Control Protocol (MCP) server.
 * Configures and initializes the MCP server with either SSE or stdio transport.
 * Only activated when the 'mcp.server.name' property is present.
 */
@Slf4j
@AutoConfiguration
@AutoConfigureOrder
@ConditionalOnProperty(prefix = "mcp.server", name = "name")
public class McpServerAutoConfiguration {
    /**
     * Endpoint path for MCP messages
     */
    private final static String MESSAGE_ENDPOINT = "/mcp/message";

    /**
     * Name identifier for the MCP server
     */
    @Value("${mcp.server.name}")
    private String name;

    /**
     * Version identifier for the MCP server
     */
    @Value("${mcp.server.version}")
    private String version;

    /**
     * Creates a WebFluxSseServerTransport bean if the transport mode is set to SSE.
     * This transport is used for handling Server-Sent Events (SSE).
     *
     * @return the WebFluxSseServerTransport instance
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.server", name = "mode", havingValue = "sse")
    public WebFluxSseServerTransportProvider sseServerTransport() {
        return new WebFluxSseServerTransportProvider(new ObjectMapper(), MESSAGE_ENDPOINT);
    }

    /**
     * Creates a RouterFunction bean for the SSE transport.
     * This router function defines the HTTP endpoints for the SSE transport.
     *
     * @param transport the WebFluxSseServerTransport instance
     * @return the configured RouterFunction
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.server", name = "mode", havingValue = "sse")
    public RouterFunction<?> mcpRouterFunction(WebFluxSseServerTransportProvider transport) {
        return transport.getRouterFunction();
    }

    /**
     * Creates a StdioServerTransport bean if the transport mode is set to stdio.
     * This transport is used for handling standard input/output communication.
     *
     * @return the StdioServerTransport instance
     */
    @Bean
    @ConditionalOnProperty(prefix = "mcp.server", name = "mode", havingValue = "stdio")
    public StdioServerTransportProvider stdioServerTransport() {
        return new StdioServerTransportProvider();
    }

    /**
     * Creates an asynchronous MCP server instance.
     * This method initializes the server with the specified transport and server information.
     * Configures server capabilities including tools support, resource management, and logging.
     *
     * @param transport the ServerMcpTransport instance (either SSE or stdio)
     * @return the configured McpAsyncServer instance
     */
    @Bean
    @ConditionalOnMissingBean
    public McpAsyncServer mcpAsyncServer(McpServerTransportProvider transport) {
        log.info("Starting MCP server {} version {} with transport: {} ",
                name,
                version,
                transport.getClass().getSimpleName());

        return McpServer.async(transport)
                .serverInfo(name, version)
                .capabilities(McpSchema.ServerCapabilities
                        .builder()
                        .tools(true)
                        .resources(false, true)
                        .logging()
                        .build())
                .build();
    }
}