package io.confluent.pas.agent.proxy.registration;

import io.confluent.kafka.schemaregistry.client.SchemaMetadata;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.RegistrationService;
import io.confluent.pas.agent.common.services.Schemas;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationCoordinatorTest {

    @Mock
    private KafkaConfiguration kafkaConfiguration;

    @Mock
    private RequestResponseHandler requestResponseHandler;

    @Mock
    private McpAsyncServer mcpServer;

    @Mock
    private SchemaRegistryClient schemaRegistryClient;

    @Mock
    private RegistrationService<Schemas.RegistrationKey, Schemas.Registration> registrationService;

    @InjectMocks
    private RegistrationCoordinator registrationCoordinator;

    @BeforeEach
    void setUp() throws RestClientException, IOException {
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

        when(schemaRegistryClient.getLatestSchemaMetadata(any())).thenReturn(new SchemaMetadata(
                1,
                1,
                "JSON",
                new ArrayList<>(),
                "{\"type\":\"record\",\"name\":\"test\",\"fields\":[{\"name\":\"field\",\"type\":\"string\"}]}"
        ));

        when(mcpServer.addTool(any(McpServerFeatures.AsyncToolSpecification.class))).thenReturn(Mono.empty());

        doNothing().when(registrationService).register(any(), any());

        registrationCoordinator = new RegistrationCoordinator(
                requestResponseHandler,
                mcpServer,
                schemaRegistryClient,
                registrationService,
                mock(ApplicationEventPublisher.class));
    }

    @Test
    void testIsRegistered() {
        String toolName = "testTool";
        assertFalse(registrationCoordinator.isRegistered(toolName));

        registrationCoordinator.register(new Schemas.Registration(toolName, "description", "request", "response"));

        verify(registrationService, times(1)).register(any(Schemas.RegistrationKey.class), any(Schemas.Registration.class));
    }

    @Test
    void testGetRegistrationHandler() {
        String toolName = "testTool";
        assertNull(registrationCoordinator.getRegistrationHandler(toolName));

        var registration = new Schemas.Registration(toolName, "description", "request", "response");

        registrationCoordinator.onRegistration(Map.of(new Schemas.RegistrationKey(toolName), registration));
        assertNotNull(registrationCoordinator.getRegistrationHandler(toolName));
    }

    @Test
    void testGetAllRegistrationHandlers() {
        assertTrue(registrationCoordinator.getAllRegistrationHandlers().isEmpty());

        registrationCoordinator.register(new Schemas.Registration("testTool1", "description", "request", "response"));
        registrationCoordinator.register(new Schemas.Registration("testTool2", "description", "request", "response"));

        verify(registrationService, times(2)).register(any(Schemas.RegistrationKey.class), any(Schemas.Registration.class));
    }

    @Test
    void testRegister() {
        Schemas.Registration registration = new Schemas.Registration("testTool", "description", "request", "response");
        registrationCoordinator.register(registration);

        verify(registrationService, times(1)).register(any(Schemas.RegistrationKey.class), eq(registration));
    }

    @Test
    void testUnregister() {
        String toolName = "testTool";
        registrationCoordinator.register(new Schemas.Registration(toolName, "description", "request", "response"));
        registrationCoordinator.unregister(toolName);

        verify(registrationService, times(1)).unregister(any(Schemas.RegistrationKey.class));
    }

    @Test
    void testOnRegistration() {
        Schemas.RegistrationKey key = new Schemas.RegistrationKey("testTool");
        Schemas.Registration registration = new Schemas.Registration("testTool", "description", "request", "response");
        Map<Schemas.RegistrationKey, Schemas.Registration> registrations = Collections.singletonMap(key, registration);

        registrationCoordinator.onRegistration(registrations);

        verify(requestResponseHandler, times(1)).addRegistrations(anyCollection());
    }

    @Test
    void testDestroy() {
        registrationCoordinator.destroy();
        verify(registrationService, times(1)).close();
    }
}