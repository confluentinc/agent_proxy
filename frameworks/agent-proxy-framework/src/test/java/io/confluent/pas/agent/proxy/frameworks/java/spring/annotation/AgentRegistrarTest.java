package io.confluent.pas.agent.proxy.frameworks.java.spring.annotation;

import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.frameworks.java.SubscriptionHandler;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AgentRegistrarTest {

    record Request(int a, int b) {
    }

    @Getter
    @Setter
    static class Response extends Schemas.ResourceResponse {
        public int result;
    }


    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private KafkaConfiguration kafkaConfiguration;

    @Mock
    private SubscriptionHandler<?, ?, ?> subscriptionHandler;

    private AgentRegistrar agentRegistrar;

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

        agentRegistrar = new AgentRegistrar(applicationContext, (keyClass, requestClass, responseClass) -> subscriptionHandler);
    }

    @Test
    public void testAfterPropertiesSet() throws Exception {
        // Mock beans and methods
        Object bean = new TestBean();
        when(applicationContext.getBeanDefinitionNames()).thenReturn(new String[]{"testBean"});
        when(applicationContext.getBean("testBean")).thenReturn(bean);

        // Execute the method
        agentRegistrar.afterPropertiesSet();

        // Verify the subscription handler is created and subscribed
        ArgumentCaptor<Schemas.Registration> registrationCaptor = ArgumentCaptor.forClass(Schemas.Registration.class);
        verify(subscriptionHandler, times(2)).subscribeWith(registrationCaptor.capture(), any());

        List<Schemas.Registration> registrations = registrationCaptor.getAllValues();
        assertEquals(2, registrations.size());
        assertEquals("testAgent", registrations.get(0).getName());
    }

    @Test
    public void testClose() {
        // Add a handler to the list
        AgentRegistrar.InvocationHandler handler = mock(AgentRegistrar.InvocationHandler.class);
        agentRegistrar.addHandler(handler);

        // Execute the method
        agentRegistrar.close();

        // Verify the handler is closed
        verify(handler, times(1)).close();
    }

    public static class TestBean {
        @Agent(name = "testAgent",
                description = "Test Agent",
                request_topic = "testRequest",
                response_topic = "testResponse",
                requestClass = Request.class,
                responseClass = Response.class)
        public void testAgentMethod() {
        }

        @Resource(name = "testResource",
                description = "Test Resource",
                request_topic = "testRequest",
                response_topic = "testResponse",
                contentType = "application/json",
                path = "/test",
                responseClass = Response.class)
        public void testResourceMethod() {
        }
    }
}