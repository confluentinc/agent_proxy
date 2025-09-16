package io.confluent.pas.agent.proxy.registration.handlers.mcp;

import io.confluent.pas.agent.common.services.schemas.ResourceRegistration;
import io.confluent.pas.agent.common.services.schemas.ResourceResponse;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.confluent.pas.agent.proxy.registration.handlers.AbstractRegistrationHandler;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.confluent.pas.mcp.common.schemas.BlobResourceResponse;
import io.confluent.pas.mcp.common.schemas.TextResourceResponse;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import javax.naming.OperationNotSupportedException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Handler for Model Context Protocol (MCP) resource registration and request
 * processing.
 * This class manages the lifecycle of MCP resources, including registration,
 * request routing,
 * and response handling. It supports both blob and text-based resource types.
 */
@Slf4j
public class McpResourceHandler extends AbstractRegistrationHandler<ResourceRegistration, McpAsyncServer, McpSchema.ReadResourceResult> {

    private static final String TEMPLATE_NOT_SUPPORTED_MESSAGE = "Resource template registration is not supported";

    /**
     * Constructs a new McpResourceHandler with the specified registration
     * information and servers.
     *
     * @param registration           The resource registration information
     * @param schemas                The registration schemas
     * @param requestResponseHandler The handler for processing requests and
     *                               responses
     * @param mcpServer              The MCP async server for resource registration
     */
    public McpResourceHandler(ResourceRegistration registration,
                              RegistrationSchemas schemas,
                              RequestResponseHandler requestResponseHandler,
                              McpAsyncServer mcpServer) {
        super(registration, schemas, mcpServer, requestResponseHandler, (payload) -> {
            final String responseType = payload.get("type").asText();

            final McpSchema.ResourceContents content = createResourceContents(
                    JsonUtils.toMap(payload),
                    responseType);
            return new McpSchema.ReadResourceResult(List.of(content));
        });
    }

    /**
     * Initializes the resource by registering it with the MCP server.
     * Template registrations are currently not supported and will result in an
     * error.
     *
     * @return A Mono that completes when the resource has been registered
     */
    @Override
    public Mono<Void> initialize() {
        log.info("Registering resource {}", registration.getName());

        final McpSchema.Annotations annotations = createAnnotations();

        if (registration.isTemplate()) {
            log.error(TEMPLATE_NOT_SUPPORTED_MESSAGE);
            return Mono.error(new OperationNotSupportedException(TEMPLATE_NOT_SUPPORTED_MESSAGE));
        }

        return registrationServer.addResource(createResourceRegistration(annotations));
    }

    /**
     * Tears down the resource by removing it from the MCP server.
     *
     * @return A Mono that completes when the resource has been removed
     */
    @Override
    public Mono<Void> teardown() {
        return registrationServer.removeResource(registration.getName());
    }

    /**
     * Creates MCP annotations for the resource, specifying roles and weights.
     *
     * @return The created annotations
     */
    private McpSchema.Annotations createAnnotations() {
        return new McpSchema.Annotations(
                List.of(McpSchema.Role.ASSISTANT, McpSchema.Role.USER),
                1.0);
    }

    /**
     * Creates an async resource specification for the MCP server.
     * This defines how the server should handle resource requests.
     *
     * @param annotations The annotations to apply to the resource
     * @return The async resource specification
     */
    private McpServerFeatures.AsyncResourceSpecification createResourceRegistration(McpSchema.Annotations annotations) {
        McpSchema.Resource resource = new McpSchema.Resource(
                registration.getUrl(),
                registration.getName(),
                registration.getDescription(),
                registration.getMimeType(),
                annotations);

        return new McpServerFeatures.AsyncResourceSpecification(
                resource,
                (exchange, rcsRequest) -> onRequest(JsonUtils.toMap(rcsRequest)));
    }

    /**
     * Creates resource contents based on the response type (blob or text).
     * Acts as a factory method to delegate to the appropriate content creator.
     *
     * @param response     The response payload
     * @param responseType The type of response (BLOB or TEXT)
     * @return The created resource contents
     */
    private static McpSchema.ResourceContents createResourceContents(
            Map<String, Object> response,
            String responseType) {

        if (Objects.equals(responseType, ResourceResponse.BLOB_TYPE)) {
            return createBlobContents(response);
        } else {
            return createTextContents(response);
        }
    }

    /**
     * Creates blob resource contents from the response.
     *
     * @param response The response payload
     * @return The blob resource contents
     */
    private static McpSchema.BlobResourceContents createBlobContents(Map<String, Object> response) {
        BlobResourceResponse resource = JsonUtils.toObject(response, BlobResourceResponse.class);
        return new McpSchema.BlobResourceContents(
                resource.getUri(),
                resource.getMimeType(),
                resource.getBlob());
    }

    /**
     * Creates text resource contents from the response.
     *
     * @param response The response payload
     * @return The text resource contents
     */
    private static McpSchema.TextResourceContents createTextContents(Map<String, Object> response) {
        TextResourceResponse resource = JsonUtils.toObject(response, TextResourceResponse.class);
        return new McpSchema.TextResourceContents(
                resource.getUri(),
                resource.getMimeType(),
                resource.getText());
    }
}
