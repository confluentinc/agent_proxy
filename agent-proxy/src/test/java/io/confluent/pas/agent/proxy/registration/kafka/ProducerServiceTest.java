package io.confluent.pas.agent.proxy.registration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.pas.agent.common.utils.Lazy;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProducerServiceTest {

    @Mock
    private KafkaProducer<JsonNode, JsonNode> kafkaProducer;

    private ProducerService producerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        var lazyProducer = new Lazy<>(() -> kafkaProducer);
        lazyProducer.get();

        producerService = new ProducerService(lazyProducer);
    }

    @Test
    void testSend() throws Exception {
        String topic = "test-topic";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode key = mapper.readTree("{\"key\": \"value\"}");
        JsonNode value = mapper.readTree("{\"value\": \"test\"}");

        Future<RecordMetadata> future = mock(Future.class);
        when(kafkaProducer.send(any(ProducerRecord.class), any(Callback.class)))
                .thenAnswer(
                        invocationOnMock -> {
                            ((Callback) invocationOnMock.getArguments()[1]).onCompletion(
                                    mock(RecordMetadata.class),
                                    null
                            );

                            return null;
                        }
                )
                .thenReturn(future);

        Mono<Void> result = producerService.send(topic, key, value);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        ArgumentCaptor<ProducerRecord> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaProducer).send(recordCaptor.capture(), any());

        ProducerRecord<JsonNode, JsonNode> capturedRecord = recordCaptor.getValue();
        assertEquals(topic, capturedRecord.topic());
        assertEquals(key, capturedRecord.key());
        assertEquals(value, capturedRecord.value());
    }

    @Test
    void testSendWithException() throws Exception {
        String topic = "test-topic";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode key = mapper.readTree("{\"key\": \"value\"}");
        JsonNode value = mapper.readTree("{\"value\": \"test\"}");

        when(kafkaProducer.send(any(ProducerRecord.class), any(Callback.class)))
                .thenAnswer(invocation -> {
                    ((Callback) invocation.getArgument(1))
                            .onCompletion(null, new RuntimeException("Test exception"));
                    return null;
                });

        Mono<Void> result = producerService.send(topic, key, value);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testClose() {
        producerService.close();
        verify(kafkaProducer).close();
    }
}