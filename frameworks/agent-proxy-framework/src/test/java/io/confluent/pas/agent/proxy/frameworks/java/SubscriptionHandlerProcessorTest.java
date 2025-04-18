package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.pas.agent.proxy.frameworks.java.SubscriptionHandler.RequestHandler;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class SubscriptionHandlerProcessorTest {

    @Mock
    private RequestHandler<Key, String, String> requestHandler;

    @Mock
    private ProcessorContext<Key, String> context;

    private SubscriptionHandlerProcessor<Key, String, String> processor;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new SubscriptionHandlerProcessor<>(requestHandler);
        processor.init(context);
    }


    @Test
    public void testProcess() {
        Key key = new Key("testKey");
        String value = "testValue";
        Record<Key, String> record = new Record<>(key, value, System.currentTimeMillis());

        processor.process(record);

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(requestHandler, times(1)).onRequest(requestCaptor.capture());

        Request<Key, String, String> capturedRequest = requestCaptor.getValue();
        assertEquals(key, capturedRequest.getKey());
        assertEquals(value, capturedRequest.getRequest());
    }

    @Test
    public void testSendResponse() {
        Key key = new Key("testKey");
        String responseValue = "responseValue";
        Response<Key, String> response = new Response<>(key, responseValue);

        processor.sendResponse(response);

        ArgumentCaptor<Record> recordCaptor = ArgumentCaptor.forClass(Record.class);
        verify(context, times(1)).forward(recordCaptor.capture());

        Record<Key, String> capturedRecord = recordCaptor.getValue();
        assertEquals(key, capturedRecord.key());
        assertEquals(responseValue, capturedRecord.value());
    }
}