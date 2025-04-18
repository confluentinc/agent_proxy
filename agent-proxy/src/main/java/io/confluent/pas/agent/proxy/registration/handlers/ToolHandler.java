package io.confluent.pas.agent.proxy.registration.handlers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.registration.RegistrationHandler;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Handle the registration for a tool
 */
@Slf4j
@AllArgsConstructor
public class ToolHandler implements RegistrationHandler<Map<String, Object>, JsonNode> {
    @Getter
    private final Schemas.Registration registration;
    @Getter
    private final RegistrationSchemas schemas;
    private final RequestResponseHandler requestResponseHandler;

    public ToolHandler(Schemas.Registration registration,
                       SchemaRegistryClient schemaRegistryClient,
                       RequestResponseHandler requestResponseHandler) throws RestClientException, IOException {
        this.requestResponseHandler = requestResponseHandler;
        this.registration = registration;
        this.schemas = new RegistrationSchemas(schemaRegistryClient, registration);
    }

    /**
     * Register the tool with the server
     *
     * @param mcpServer the server to register with
     * @return a mono that completes when the registration is complete
     */
    public Mono<Void> register(McpAsyncServer mcpServer) {
        final McpSchema.Tool tool = new McpSchema.Tool(
                registration.getName(),
                registration.getDescription(),
                schemas.getRequestSchema().getSchema());

        log.info("Registering tool {}", registration.getName());
        final McpServerFeatures.AsyncToolSpecification toolRegistration = new McpServerFeatures.AsyncToolSpecification(tool,
                (exchange, toolArguments) -> Mono.create(sink -> sendToolRequest(toolArguments, sink)));

        return mcpServer.addTool(toolRegistration);
    }

    /**
     * Unregister the tool from the server
     *
     * @param mcpServer the server to unregister from
     * @return a mono that completes when the un-registration is complete
     */
    public Mono<Void> unregister(McpAsyncServer mcpServer) {
        return mcpServer.removeTool(registration.getName());
    }

    /**
     * Send a request to the tool
     *
     * @param arguments the arguments to send
     * @return the response
     */
    public Mono<JsonNode> sendRequest(Map<String, Object> arguments) {
        final String correlationId = UUID.randomUUID().toString();

        try {
            // Send the request
            return requestResponseHandler
                    .sendRequestResponse(registration,
                            schemas,
                            correlationId,
                            arguments);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Failed to send request", e);
            return Mono.error(e);
        }
    }

    /**
     * Send a request to the tool
     *
     * @param arguments the arguments to send
     * @param sink      the sink to send the response to
     */
    protected void sendToolRequest(Map<String, Object> arguments, MonoSink<McpSchema.CallToolResult> sink) {
        sendRequest(arguments).subscribe(response -> {
            // Serialize the response
            try {
                final String result = JsonUtils.toString(response);
                sink.success(new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(result)),
                        false));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize response", e);
                sink.error(e);
            }
        });
    }
}
