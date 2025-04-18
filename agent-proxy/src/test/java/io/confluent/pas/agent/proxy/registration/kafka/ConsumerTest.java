package io.confluent.pas.agent.proxy.registration.kafka;

import io.confluent.pas.agent.common.services.KafkaConfiguration;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumerTest {

    @Mock
    private KafkaConsumer<String, String> kafkaConsumer;

    @Mock
    private ConsumerHandler<String, String> consumerHandler;

    @Mock
    private KafkaConfiguration kafkaConfiguration;

    private Consumer<String, String> consumer;

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


        consumer = new Consumer<>(consumerHandler, kafkaConsumer, mock(Consumer.TimeoutChecker.class));
    }

    @Test
    void testSubscribe() {
        consumer.subscribe("test-topic");
        assertTrue(consumer.isSubscribed("test-topic"));
    }

    @Test
    void testUnsubscribe() {
        consumer.subscribe("test-topic");
        consumer.unsubscribe("test-topic");
        assertFalse(consumer.isSubscribed("test-topic"));
    }

    @Test
    void testClose() throws IOException {
        consumer.close();
        verify(kafkaConsumer).wakeup();
    }

    @Test
    void testProcessRecord() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("test-topic", 0, 0, "key", "value");
        consumer.subscribe("test-topic");
        consumer.processRecord(record);
        verify(consumerHandler).onMessage("test-topic", "key", "value");
    }

    @Test
    void testRunLoop() throws InterruptedException, IOException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                consumer.runLoop();
            } catch (WakeupException e) {
                // Expected exception on close
            }
        });

        Thread.sleep(100); // Allow some time for the loop to start
        consumer.close();
        executorService.shutdown();
        assertTrue(executorService.isShutdown());
    }
}