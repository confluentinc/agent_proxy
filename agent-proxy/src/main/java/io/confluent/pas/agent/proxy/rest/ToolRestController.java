package io.confluent.pas.agent.proxy.rest;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.RegistrationHandler;
import io.confluent.pas.agent.proxy.registration.handlers.ResourceHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Controller responsible for handling tool and resource requests.
 */
@Slf4j
@Component
public class ToolRestController {

    // Type reference for deserializing request body to a Map
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {
    };

    // Coordinator for managing registrations
    private final RegistrationCoordinator registrationCoordinator;

    // Map to store resource handlers by URL pattern
    private final Map<String, ResourceHandler> resourceHandlersByUrlPattern = new HashMap<>();

    /**
     * Constructor that initializes the controller and sets up resource handlers.
     *
     * @param registrationCoordinator the coordinator for managing registrations
     */
    public ToolRestController(RegistrationCoordinator registrationCoordinator) {
        this.registrationCoordinator = registrationCoordinator;
        initializeResourceHandlers();
    }

    /**
     * Initializes the resource handlers map based on registered handlers.
     */
    private void initializeResourceHandlers() {
        registrationCoordinator.getAllRegistrationHandlers()
                .stream()
                .filter(handler -> handler instanceof ResourceHandler)
                .map(handler -> (ResourceHandler) handler)
                .forEach(handler -> {
                    final Schemas.ResourceRegistration registration = handler.getRegistration();
                    final String urlPattern = registration.getUrl();
                    resourceHandlersByUrlPattern.put(urlPattern, handler);
                    log.debug("Registered resource handler for pattern: {}", urlPattern);
                });
    }

    /**
     * Process a request for a tool.
     *
     * @param request the server request
     * @return the server response
     */
    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> processRequest(ServerRequest request) {
        final String toolName = request.pathVariable("toolName");
        log.debug("Processing request for tool: {}", toolName);

        // Check if the tool is registered
        if (!registrationCoordinator.isRegistered(toolName)) {
            log.error("Tool {} is not registered", toolName);
            return createErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    String.format("Tool '%s' is not registered", toolName));
        }

        // Get the registration handler for the tool
        final RegistrationHandler<Map<String, Object>, JsonNode> handler = (RegistrationHandler<Map<String, Object>, JsonNode>) registrationCoordinator
                .getRegistrationHandler(toolName);

        // Process the request body and send it to the handler
        return request.bodyToMono(MAP_TYPE)
                .doOnNext(arguments -> log.info("Received request for tool {}", toolName))
                .flatMap(args -> {
                    try {
                        return handler.sendRequest(args)
                                .doOnError(e -> log.error("Error processing request for tool {}: {}",
                                        toolName, e.getMessage(), e));
                    } catch (Exception e) {
                        log.error("Exception processing request for tool {}: {}",
                                toolName, e.getMessage(), e);
                        return Mono.error(e);
                    }
                })
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(response))
                .onErrorResume(e -> createErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        String.format("Error processing request for tool '%s': %s",
                                toolName, e.getMessage())));
    }

    /**
     * Process a resource request.
     *
     * @param request the server request
     * @return the server response
     */
    public Mono<ServerResponse> processResourceRequest(ServerRequest request) {
        final List<String> urlParts = extractUrlParts(request);
        final String requestPath = request.path();

        log.debug("Processing resource request for path: {}", requestPath);

        // Get the appropriate handler for the resource request
        return findResourceHandler(urlParts)
                .map(handler -> {
                    final Schemas.ResourceRequest resourceRequest = new Schemas.ResourceRequest(
                            StringUtils.join(urlParts, "/"));

                    // Send the resource request to the handler
                    return handler.sendRequest(resourceRequest)
                            .doOnError(e -> log.error("Error sending request to resource {}: {}",
                                    requestPath, e.getMessage(), e))
                            .flatMap(this::createResourceResponse)
                            .onErrorResume(e -> createErrorResponse(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    String.format("Error processing resource request: %s", e.getMessage())));
                })
                .orElseGet(() -> {
                    log.error("No handler found for {}", requestPath);
                    return createErrorResponse(
                            HttpStatus.NOT_FOUND,
                            String.format("No handler found for '%s'", requestPath));
                });
    }

    /**
     * Extract the URL parts from the request path.
     *
     * @param request the server request
     * @return the list of URL parts
     */
    private List<String> extractUrlParts(ServerRequest request) {
        return Stream.of(request.path().split("/"))
                .filter(StringUtils::isNotEmpty)
                .skip(1) // Skip the initial part which is empty
                .toList();
    }

    /**
     * Create a server response from the resource response.
     *
     * @param response the resource response
     * @return the server response Mono
     */
    private Mono<ServerResponse> createResourceResponse(Schemas.ResourceResponse response) {
        final MediaType mediaType = MediaType.parseMediaType(response.getMimeType());
        final String responseContent = extractResponseContent(response);

        return ServerResponse.ok()
                .contentType(mediaType)
                .bodyValue(responseContent);
    }

    /**
     * Extract the content from the resource response.
     *
     * @param response the resource response
     * @return the response content as a string
     */
    private String extractResponseContent(Schemas.ResourceResponse response) {
        if (response instanceof Schemas.BlobResourceResponse blobResponse) {
            return blobResponse.getBlob();
        } else if (response instanceof Schemas.TextResourceResponse textResponse) {
            return textResponse.getText();
        } else {
            throw new IllegalArgumentException("Unsupported resource response type: " + response.getClass().getName());
        }
    }

    /**
     * Create an error response with the given status and message.
     *
     * @param status  the HTTP status
     * @param message the error message
     * @return the server response Mono
     */
    private Mono<ServerResponse> createErrorResponse(HttpStatus status, String message) {
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", status.getReasonPhrase(), "message", message));
    }

    /**
     * Find the handler for the given URL parts.
     *
     * @param urlParts the URL parts
     * @return an Optional containing the handler if found, empty otherwise
     */
    private Optional<RegistrationHandler<Schemas.ResourceRequest, Schemas.ResourceResponse>> findResourceHandler(
            List<String> urlParts) {
        return resourceHandlersByUrlPattern.keySet().stream()
                .filter(pattern -> isUrlPatternMatch(pattern, urlParts))
                .map(pattern -> (RegistrationHandler<Schemas.ResourceRequest, Schemas.ResourceResponse>) resourceHandlersByUrlPattern
                        .get(pattern))
                .findFirst();
    }

    /**
     * Check if the given URL pattern matches the URL parts.
     *
     * @param urlPattern the URL pattern
     * @param urlParts   the URL parts
     * @return true if the pattern matches the URL parts
     */
    private boolean isUrlPatternMatch(String urlPattern, List<String> urlParts) {
        final List<String> patternParts = Stream.of(urlPattern.split("/"))
                .filter(StringUtils::isNotEmpty)
                .toList();

        if (urlParts.size() != patternParts.size()) {
            return false;
        }

        return Stream.iterate(0, i -> i + 1)
                .limit(urlParts.size())
                .allMatch(i -> isPartMatch(patternParts.get(i), urlParts.get(i)));
    }

    /**
     * Check if a pattern part matches a URL part.
     *
     * @param patternPart the pattern part
     * @param urlPart     the URL part
     * @return true if the pattern part matches the URL part
     */
    private boolean isPartMatch(String patternPart, String urlPart) {
        return (patternPart.startsWith("{") && patternPart.endsWith("}")) ||
                patternPart.equals(urlPart);
    }
}