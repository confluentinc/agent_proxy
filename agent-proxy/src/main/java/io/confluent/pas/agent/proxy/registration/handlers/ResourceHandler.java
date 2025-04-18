package io.confluent.pas.agent.proxy.registration.handlers;

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

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@AllArgsConstructor
public class ResourceHandler implements RegistrationHandler<Schemas.ResourceRequest, Schemas.ResourceResponse> {
    @Getter
    private final Schemas.ResourceRegistration registration;
    @Getter
    private final RegistrationSchemas schemas;

    private final RequestResponseHandler requestResponseHandler;

    public ResourceHandler(Schemas.ResourceRegistration registration,
                           SchemaRegistryClient schemaRegistryClient,
                           RequestResponseHandler requestResponseHandler) throws RestClientException, IOException {
        this.requestResponseHandler = requestResponseHandler;
        this.registration = registration;
        this.schemas = new RegistrationSchemas(schemaRegistryClient, registration);
    }

    /**
     * Register the resource with the server
     *
     * @param mcpServer the server to register with
     * @return a mono that completes when the registration is complete
     */
    public Mono<Void> register(McpAsyncServer mcpServer) throws OperationNotSupportedException {
        log.info("Registering resource {}", registration.getName());

        final McpSchema.Annotations annotations = new McpSchema.Annotations(
                List.of(McpSchema.Role.ASSISTANT, McpSchema.Role.USER),
                1.0
        );

        if (registration.isTemplate()) {
            log.error("Resource template registration is not supported");
            return Mono.error(new OperationNotSupportedException("Resource template registration is not supported"));
//            return mcpServer.addResourceTemplate(getAsyncResourceTemplateRegistration(annotations));
        }

        return mcpServer.addResource(getAsyncResourceRegistration(annotations));
    }

    /**
     * Get the async resource registration
     *
     * @param annotations the annotations for the resource
     * @return the async resource registration
     */
    McpServerFeatures.AsyncResourceSpecification getAsyncResourceRegistration(McpSchema.Annotations annotations) {
        McpSchema.Resource resource = new McpSchema.Resource(
                registration.getUrl(),
                registration.getName(),
                registration.getDescription(),
                registration.getMimeType(),
                annotations
        );

        return new McpServerFeatures.AsyncResourceSpecification(
                resource,
                (arguments, rcsRequest) -> Mono.create(sink -> sendRequest(
                        rcsRequest,
                        sink)));
    }

//    /**
//     * Get the async resource template registration
//     *
//     * @param annotations the annotations for the resource
//     * @return the async resource template registration
//     */
//    private McpServerFeatures.AsyncResourceTemplateSpecification getAsyncResourceTemplateRegistration(McpSchema.Annotations annotations) {
//        McpSchema.ResourceTemplate template = new McpSchema.ResourceTemplate(
//                registration.getUrl(),
//                registration.getName(),
//                registration.getDescription(),
//                registration.getMimeType(),
//                annotations
//        );
//
//        return new McpServerFeatures.AsyncResourceTemplateSpecification(
//                template,
//                (exchange, arguments) -> Mono.create(sink -> sendRequest(
//                        arguments,
//                        sink)));
//    }

    @Override
    public Mono<Void> unregister(McpAsyncServer mcpServer) {
        return mcpServer.removeResource(registration.getName());
    }

    @Override
    public Mono<Schemas.ResourceResponse> sendRequest(Schemas.ResourceRequest request) {
        final Map<String, Object> arguments = JsonUtils.toMap(request);

        return sendRequest(arguments)
                .map(response -> JsonUtils.toObject(response, Schemas.ResourceResponse.class));
    }

    /**
     * Send a request to the resource
     *
     * @param arguments the request arguments
     * @return a mono containing the response
     */
    protected Mono<JsonNode> sendRequest(Map<String, Object> arguments) {
        final String correlationId = UUID.randomUUID().toString();

        try {
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
     * Send a request to the resource
     *
     * @param request the read resource request
     * @param sink    the sink to send the response to
     */
    protected void sendRequest(McpSchema.ReadResourceRequest request, MonoSink<McpSchema.ReadResourceResult> sink) {
        final Map<String, Object> arguments = JsonUtils.toMap(request);

        sendRequest(arguments).subscribe(response -> {
            final Schemas.ResourceResponse.ResponseType responseType = Schemas.ResourceResponse.ResponseType.fromValue(response.get("type").asText());

            final McpSchema.ResourceContents content;
            if (responseType == Schemas.ResourceResponse.ResponseType.BLOB) {
                Schemas.BlobResourceResponse resource = JsonUtils.toObject(response, Schemas.BlobResourceResponse.class);
                content = new McpSchema.BlobResourceContents(
                        resource.getUri(),
                        resource.getMimeType(),
                        resource.getBlob());
            } else {
                Schemas.TextResourceResponse resource = JsonUtils.toObject(response, Schemas.TextResourceResponse.class);
                content = new McpSchema.TextResourceContents(
                        resource.getUri(),
                        resource.getMimeType(),
                        resource.getText());
            }

            sink.success(new McpSchema.ReadResourceResult(List.of(content)));
        });
    }
}