package io.confluent.pas.agent.proxy;

import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.frameworks.java.SubscriptionHandler;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.RegistrationHandler;
import io.confluent.pas.agent.proxy.registration.RequestResponseHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@EnableAsync
@Testcontainers
@SpringBootTest
public class TestProxyIT {

    private static final String KAFKA_IMAGE = "apache/kafka:3.7.0";

    private final CountDownLatch latch = new CountDownLatch(1);

    @Container
    private static final KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE)
            .withStartupTimeout(Duration.ofMinutes(2))
            .withStartupAttempts(3);

    @DynamicPropertySource
    public static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.broker-servers", kafka::getBootstrapServers);
    }

    private final KafkaConfiguration kafkaConfiguration;
    private final RegistrationCoordinator coordinator;
    private final ProxyEventListener listener;
    private final RequestResponseHandler requestResponseHandler;

    @Autowired
    public TestProxyIT(KafkaConfiguration kafkaConfiguration,
                       RegistrationCoordinator coordinator,
                       ProxyEventListener listener,
                       RequestResponseHandler requestResponseHandler) {
        this.kafkaConfiguration = kafkaConfiguration;
        this.coordinator = coordinator;
        this.listener = listener;
        this.requestResponseHandler = requestResponseHandler;
    }

    @Test
    public void testToolRegistration() throws InterruptedException {
        listener.reset();

        final SubscriptionHandler<Key, String, String> handler = new SubscriptionHandler<>(
                kafkaConfiguration,
                Key.class,
                String.class,
                String.class);

        Schemas.Registration registration = new Schemas.Registration(
                "test",
                "Sample registration",
                "sample_req",
                "sample_res");

        handler.subscribeWith(registration, (req) -> {
            System.out.println("Received registration");
        });

        listener.waitForEvent();

        final List<RegistrationHandler<?, ?>> handlers = coordinator.getAllRegistrationHandlers();
        assertThat(handlers).isNotNull();
        assertThat(handlers.size()).isEqualTo(1);
    }
}
