package io.confluent.pas.agent.proxy.registration;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.proxy.registration.kafka.ProducerService;
import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.registration.kafka.ConsumerService;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Handle requests and responses
 */
@Slf4j
@Component
public class RequestResponseHandler implements DisposableBean {

    private final ProducerService producerService;
    private final ConsumerService consumerService;
    private final ObservationRegistry observationRegistry;

    @Autowired
    public RequestResponseHandler(KafkaConfiguration kafkaConfiguration,
                                  ObservationRegistry observationRegistry,
                                  @Value("${kafka.response.timeout:10000}") long responseTimeout) {
        this(new ProducerService(kafkaConfiguration),
                new ConsumerService(kafkaConfiguration, responseTimeout),
                observationRegistry);
    }

    public RequestResponseHandler(ProducerService producerService,
                                  ConsumerService consumerService,
                                  ObservationRegistry observationRegistry) {
        this.producerService = producerService;
        this.consumerService = consumerService;
        this.observationRegistry = observationRegistry;
    }

    public void addRegistrations(Collection<Schemas.Registration> registrations) {
        consumerService.addRegistrations(registrations);
    }

    /**
     * Send a request to a topic and wait for a response
     *
     * @param registration  the registration
     * @param schemas       the schemas
     * @param correlationId the correlation id
     * @param request       the request
     * @return the response
     * @throws ExecutionException   if the request fails
     * @throws InterruptedException if the request is interrupted
     */
    public Mono<JsonNode> sendRequestResponse(Schemas.Registration registration,
                                              RegistrationSchemas schemas,
                                              String correlationId,
                                              Map<String, Object> request)
            throws ExecutionException, InterruptedException {
        final Map<String, Object> key = Map.of(registration.getCorrelationIdFieldName(), correlationId);

        final Observation observation = Observation.start("agent.proxy." + registration.getName(), observationRegistry)
                .contextualName("sendRequestResponse")
                .lowCardinalityKeyValue("correlationId", correlationId)
                .highCardinalityKeyValue("name", registration.getName());

        return observation.observe(() -> sendRequestResponse(
                        registration,
                        correlationId,
                        schemas.getRequestKeySchema().envelope(key),
                        schemas.getRequestSchema().envelope(request)))
                .doOnError(observation::error)
                .doFinally(signalType -> observation.stop());
    }

    /**
     * Send a request to a topic and wait for a response
     *
     * @param registration  the registration
     * @param correlationId the correlation id
     * @param key           the key
     * @param request       the request
     * @return the response
     */
    public Mono<JsonNode> sendRequestResponse(Schemas.Registration registration,
                                              String correlationId,
                                              JsonNode key,
                                              JsonNode request) {
        Sinks.One<JsonNode> sink = Sinks.one();

        // Register the response handler
        consumerService.registerResponseHandler(
                registration,
                correlationId,
                sink::tryEmitValue,
                sink::tryEmitError);

        // Send the request
        return producerService.send(registration.getRequestTopicName(), key, request)
                .doOnError(sink::tryEmitError)
                .doOnSuccess(metadata -> log.info("Sent request to topic: {}", registration.getRequestTopicName()))
                .then(sink.asMono());
    }

    @Override
    public void destroy() throws Exception {
        consumerService.close();
        producerService.close();
    }
}
