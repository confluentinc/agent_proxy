package io.confluent.pas.agent.proxy.registration.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceHandlerTest {

    @Mock
    private Schemas.ResourceRegistration registration;

    @Mock
    private SchemaRegistryClient schemaRegistryClient;

    @Mock
    private RequestResponseHandler requestResponseHandler;

    @Mock
    private McpAsyncServer mcpServer;

    private ResourceHandler resourceHandler;

    @BeforeEach
    void setUp() throws RestClientException, IOException {
        MockitoAnnotations.openMocks(this);
        resourceHandler = new ResourceHandler(registration, schemaRegistryClient, requestResponseHandler);
    }

//    @Test
//    void testRegister() throws OperationNotSupportedException {
//        when(registration.getUrl()).thenReturn("test-url");
//        when(registration.getName()).thenReturn("test-resource");
//        when(registration.isTemplate()).thenReturn(false);
//
//        McpSchema.Annotations annotations = new McpSchema.Annotations(
//                List.of(McpSchema.Role.ASSISTANT, McpSchema.Role.USER),
//                1.0
//        );
//
//        McpServerFeatures.AsyncResourceSpecification asyncResourceRegistration = resourceHandler.getAsyncResourceRegistration(annotations);
//
//        when(mcpServer.addResource(argThat(p -> p.resource().uri().equals(asyncResourceRegistration.resource().uri()))))
//                .thenReturn(Mono.empty());
//
//        Mono<Void> result = resourceHandler.register(mcpServer);
//        assertNotNull(result);
//        verify(mcpServer).addResource(any());
//    }

    @Test
    void testUnregister() {
        when(registration.getName()).thenReturn("test-resource");

        when(mcpServer.removeResource("test-resource")).thenReturn(Mono.empty());

        Mono<Void> result = resourceHandler.unregister(mcpServer);
        assertNotNull(result);
        verify(mcpServer).removeResource("test-resource");
    }

    @Test
    void testSendRequest() throws ExecutionException, InterruptedException {
        Schemas.ResourceRequest request = new Schemas.ResourceRequest();
        Map<String, Object> arguments = Map.of("key", "value");

        when(requestResponseHandler.sendRequestResponse(any(), any(), anyString(), anyMap()))
                .thenReturn(Mono.just(mock(JsonNode.class)));

        Mono<Schemas.ResourceResponse> result = resourceHandler.sendRequest(request);
        assertNotNull(result);
        verify(requestResponseHandler).sendRequestResponse(any(), any(), anyString(), anyMap());
    }

    @Test
    void testSendRequestWithSink() throws ExecutionException, InterruptedException {
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("test-resource");
        MonoSink<McpSchema.ReadResourceResult> sink = mock(MonoSink.class);
        Map<String, Object> arguments = Map.of("key", "value");

        when(requestResponseHandler.sendRequestResponse(any(), any(), anyString(), anyMap()))
                .thenReturn(Mono.just(mock(JsonNode.class)));

        resourceHandler.sendRequest(request, sink);
        verify(requestResponseHandler).sendRequestResponse(any(), any(), anyString(), anyMap());
    }
}