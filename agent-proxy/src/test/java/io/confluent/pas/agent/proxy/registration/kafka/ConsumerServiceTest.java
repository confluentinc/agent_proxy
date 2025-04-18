package io.confluent.pas.agent.proxy.registration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.pas.agent.common.services.Schemas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumerServiceTest {

    @Mock
    private Consumer<JsonNode, JsonNode> consumer;

    private ConsumerService consumerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumerService = new ConsumerService(consumer, 10000);
    }

    @Test
    void testAddRegistrations() {
        Schemas.Registration registration = new Schemas.Registration("testTool", "testDescription", "requestTopic", "responseTopic");
        consumerService.addRegistrations(Collections.singletonList(registration));

        verify(consumer).subscribe(Collections.singletonList("responseTopic"));
    }

    @Test
    void testRegisterResponseHandler() {
        Schemas.Registration registration = new Schemas.Registration("testTool", "testDescription", "requestTopic", "responseTopic");
        ConsumerService.ResponseHandler handler = mock(ConsumerService.ResponseHandler.class);
        ConsumerService.ErrorHandler errorHandler = mock(ConsumerService.ErrorHandler.class);

        consumerService.registerResponseHandler(registration, "correlationId", handler, errorHandler);

        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(consumer).subscribe(topicCaptor.capture());
        assertEquals("responseTopic", topicCaptor.getValue());

        Map<String, ConsumerService.RegistrationItem> responseHandlers = consumerService.getResponseHandlers();
        assertTrue(responseHandlers.containsKey("responseTopic"));
        assertTrue(responseHandlers.get("responseTopic").registrationHandlers().containsKey("correlationid"));
    }

    @Test
    void testHandleResponse() throws IOException {
        Schemas.Registration registration = new Schemas.Registration("testTool", "testDescription", "requestTopic", "responseTopic");
        ConsumerService.ResponseHandler handler = mock(ConsumerService.ResponseHandler.class);
        ConsumerService.ErrorHandler errorHandler = mock(ConsumerService.ErrorHandler.class);

        consumerService.registerResponseHandler(registration, "correlationId", handler, errorHandler);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode key = mapper.readTree("{\"correlationId\": \"correlationId\"}");
        JsonNode message = mapper.readTree("{\"message\": \"testMessage\"}");

        consumerService.handleResponse("responseTopic", key, message);

        verify(handler).handle(message);
    }

    @Test
    void testClose() throws IOException {
        consumerService.close();
        verify(consumer).close();
    }
}