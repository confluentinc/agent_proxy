package io.confluent.pas.agent.proxy.registration;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.registration.kafka.ProducerService;
import io.confluent.pas.agent.proxy.registration.kafka.ConsumerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;

/**
 * Handles the request-response communication pattern using Kafka topics.
 * This component manages the lifecycle of Kafka producers and consumers,
 * handles message routing, and maintains registration of response handlers.
 * It provides functionality to send requests and handle corresponding responses
 * asynchronously using correlation IDs.
 */
@Slf4j
@Component
public class RequestResponseHandler implements DisposableBean {

    private final ProducerService producerService;
    private final ConsumerService consumerService;

    /**
     * Creates a new RequestResponseHandler with auto-configured services.
     *
     * @param kafkaConfiguration The Kafka configuration to use for producers and consumers
     * @param responseTimeout    The maximum time to wait for responses in milliseconds (default: 10000)
     */
    @Autowired
    public RequestResponseHandler(KafkaConfiguration kafkaConfiguration,
                                  @Value("${kafka.response.timeout:10000}") long responseTimeout) {
        this(new ProducerService(kafkaConfiguration),
                new ConsumerService(kafkaConfiguration, responseTimeout));
    }

    /**
     * Creates a new RequestResponseHandler with pre-configured services.
     *
     * @param producerService The service responsible for sending messages to Kafka
     * @param consumerService The service responsible for consuming messages from Kafka
     */
    public RequestResponseHandler(ProducerService producerService,
                                  ConsumerService consumerService) {
        this.producerService = producerService;
        this.consumerService = consumerService;
    }

    /**
     * Adds multiple registrations to the consumer service for message handling.
     *
     * @param registrations Collection of registrations to be added
     */
    public void addRegistrations(Collection<Registration> registrations) {
        consumerService.addRegistrations(registrations);
    }

    /**
     * Registers a response handler for a specific registration and correlation ID.
     *
     * @param registration  The registration details for the handler
     * @param correlationId The unique identifier to correlate requests with responses
     * @param handler       The handler for successful responses
     * @param errorHandler  The handler for error responses
     */
    public void registerHandler(Registration registration,
                                String correlationId,
                                ConsumerService.ResponseHandler handler,
                                ConsumerService.ErrorHandler errorHandler) {
        consumerService.registerResponseHandler(
                registration,
                correlationId,
                handler,
                errorHandler);
    }

    /**
     * Removes a previously registered response handler.
     *
     * @param registration  The registration details for the handler
     * @param correlationId The correlation ID of the handler to remove
     */
    public void unregisterHandler(Registration registration, String correlationId) {
        consumerService.unregisterResponseHandler(registration, correlationId);
    }

    /**
     * Sends a request message to the specified Kafka topic.
     *
     * @param registration The registration containing the topic information
     * @param key          The message key
     * @param request      The request payload as JsonNode
     * @return A Mono that completes when the message is sent
     */
    public Mono<Void> sendRequest(Registration registration,
                                  Key key,
                                  JsonNode request) {
        return producerService.send(registration.getRequestTopicName(), key, request);
    }

    /**
     * Implements DisposableBean to properly close Kafka resources when the bean is destroyed.
     *
     * @throws Exception if an error occurs during resource cleanup
     */
    @Override
    public void destroy() throws Exception {
        consumerService.close();
        producerService.close();
    }
}
