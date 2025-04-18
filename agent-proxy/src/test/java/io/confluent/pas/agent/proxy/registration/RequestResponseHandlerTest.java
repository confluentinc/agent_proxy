package io.confluent.pas.agent.proxy.registration;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.registration.kafka.ConsumerService;
import io.confluent.pas.agent.proxy.registration.kafka.ProducerService;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchema;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestResponseHandlerTest {

    @Mock
    private ProducerService producerService;

    @Mock
    private ConsumerService consumerService;

    @Mock
    private KafkaConfiguration kafkaConfiguration;

    @Mock
    private ObservationRegistry observationRegistry;

    private RequestResponseHandler requestResponseHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(kafkaConfiguration.applicationId()).thenReturn(RandomStringUtils.secure().nextAscii(10));
        when(kafkaConfiguration.schemaRegistryUrl()).thenReturn("mock://schema-registry");
        when(kafkaConfiguration.brokerServers()).thenReturn("mock://bootstrap-servers");
        when(kafkaConfiguration.schemaRegistryBasicAuthUserInfo()).thenReturn("user:pwd");
        when(kafkaConfiguration.saslJaasConfig()).thenReturn("sass_cfg");
        when(kafkaConfiguration.securityProtocol()).thenReturn(KafkaConfiguration.DEFAULT_SECURITY_PROTOCOL);
        when(kafkaConfiguration.registrationTopicName()).thenReturn(KafkaConfiguration.DEFAULT_REGISTRATION_TOPIC_NAME);
        when(kafkaConfiguration.topicConfiguration()).thenReturn(new KafkaConfiguration.DefaultTopicConfiguration());
        when(kafkaConfiguration.saslMechanism()).thenReturn(KafkaConfiguration.DEFAULT_SASL_MECHANISM);

        requestResponseHandler = new RequestResponseHandler(producerService, consumerService, observationRegistry);
    }

    @Test
    void testAddRegistrations() {
        Collection<Schemas.Registration> registrations = mock(Collection.class);
        requestResponseHandler.addRegistrations(registrations);
        verify(consumerService, times(1)).addRegistrations(registrations);
    }

//    @Test
//    void testSendRequestResponse() throws ExecutionException, InterruptedException {
//        Schemas.Registration registration = new Schemas.Registration("testTool", "description", "requestTopic", "responseTopic");
//        RegistrationSchemas schemas = mock(RegistrationSchemas.class);
//        String correlationId = "testCorrelationId";
//        Map<String, Object> request = Map.of("key", "value");
//
//        when(producerService.send(anyString(), any(), any())).thenReturn(Mono.empty());
//        when(schemas.getRequestKeySchema()).thenReturn(mock(RegistrationSchema.class));
//        when(schemas.getRequestSchema()).thenReturn(mock(RegistrationSchema.class));
//
//        Mono<JsonNode> response = requestResponseHandler.sendRequestResponse(registration, schemas, correlationId, request);
//
//        assertNotNull(response);
//        verify(producerService, times(1)).send(anyString(), any(), any());
//        verify(consumerService, times(1)).registerResponseHandler(eq(registration), eq(correlationId), any());
//    }

    @Test
    void testDestroy() throws Exception {
        requestResponseHandler.destroy();
        verify(consumerService, times(1)).close();
        verify(producerService, times(1)).close();
    }
}