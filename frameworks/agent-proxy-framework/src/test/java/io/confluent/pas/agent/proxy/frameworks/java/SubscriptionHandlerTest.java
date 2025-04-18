package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.RegistrationService;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.frameworks.java.kafka.TopicManagement;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

public class SubscriptionHandlerTest {

    private record Request(int a, int b) {
    }

    private record Response(int result) {
    }


    @Mock
    private KafkaConfiguration kafkaConfiguration;

    @Mock
    private RegistrationService<Schemas.RegistrationKey, Schemas.Registration> registrationService;

    @Mock
    private TopicManagement topicManagement;

    @Mock
    private KafkaStreams kStreams;

    private SubscriptionHandler<Key, Request, Response> subscriptionHandler;

    @BeforeEach
    public void setUp() {
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

        subscriptionHandler = new SubscriptionHandler<>(
                kafkaConfiguration,
                Key.class,
                Request.class,
                Response.class,
                registrationService,
                () -> topicManagement,
                (topology, properties) -> kStreams);
    }

    @Test
    public void testSubscribeWith() throws Exception {

        subscriptionHandler.subscribeWith(
                new Schemas.Registration(
                        "Name",
                        "Description",
                        "requestTopic",
                        "responseTopic",
                        "corrleationId"
                ),
                (request) -> {
                    request.respond(new Response(request.getRequest().a() + request.getRequest().b()))
                            .block();
                }
        );

        verify(topicManagement, times(1)).createTopic(eq("requestTopic"), any(Class.class), any(Class.class));
        verify(topicManagement, times(1)).createTopic(eq("responseTopic"), any(Class.class), any(Class.class));
        verify(topicManagement, times(2)).close();
        verify(registrationService, times(1)).isRegistered(any(Schemas.RegistrationKey.class));
        verify(registrationService, times(1)).register(any(Schemas.RegistrationKey.class), any(Schemas.Registration.class));
    }

    @Test
    public void testSubscribeWithSchema() throws Exception {
        final String reqSchema = "{\"type\":\"record\",\"name\":\"Request\",\"fields\":[{\"name\":\"a\",\"type\":\"int\"},{\"name\":\"b\",\"type\":\"int\"}]}";
        final String resSchema = "{\"type\":\"record\",\"name\":\"Response\",\"fields\":[{\"name\":\"result\",\"type\":\"int\"}]}";

        subscriptionHandler.subscribeWith(
                new Schemas.Registration(
                        "Name",
                        "Description",
                        "requestTopic",
                        "responseTopic",
                        "corrleationId"
                ),
                new JsonSchema(reqSchema),
                new JsonSchema(resSchema),
                (request) -> {
                    request.respond(new Response(request.getRequest().a() + request.getRequest().b()))
                            .block();
                }
        );

        verify(topicManagement, times(1)).createTopic(eq("requestTopic"), any(Class.class), any(JsonSchema.class));
        verify(topicManagement, times(1)).createTopic(eq("responseTopic"), any(Class.class), any(JsonSchema.class));
        verify(topicManagement, times(2)).close();
        verify(registrationService, times(1)).isRegistered(any(Schemas.RegistrationKey.class));
        verify(registrationService, times(1)).register(any(Schemas.RegistrationKey.class), any(Schemas.Registration.class));
    }
}
