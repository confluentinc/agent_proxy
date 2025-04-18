package io.confluent.pas.mcp.demo.mcp.connection;

import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;


/**
 * Represents a connection to an MCP server.
 */
@Getter
@AllArgsConstructor
public class McpSseConnection extends McpAbstractConnection {

    private final String url;
    private final String username;
    private final String password;

    @Override
    protected McpClientTransport getTransport() {
        WebClient.Builder webClientBuilder = WebClient
                .builder()
                .filter(ExchangeFilterFunctions
                        .basicAuthentication(username, password))
                .baseUrl(url);

        return new WebFluxSseClientTransport(webClientBuilder);
    }
}
