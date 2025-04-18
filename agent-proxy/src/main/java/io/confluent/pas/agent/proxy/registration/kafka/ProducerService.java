package io.confluent.pas.agent.proxy.registration.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.KafkaPropertiesFactory;
import io.confluent.pas.agent.common.utils.Lazy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;

import java.io.Closeable;

/**
 * ProducerService class that handles sending messages to Kafka topics.
 * This class uses a lazy-initialized KafkaProducer to send messages asynchronously.
 */
@Slf4j
@AllArgsConstructor
public class ProducerService implements Closeable {

    private final Lazy<KafkaProducer<JsonNode, JsonNode>> producer;

    public ProducerService(KafkaConfiguration kafkaConfiguration) {
        this(new Lazy<>(() -> new KafkaProducer<>(KafkaPropertiesFactory.getProducerProperties(kafkaConfiguration))));
    }

    /**
     * Send a message to a topic.
     *
     * @param topic the topic
     * @param key   the key
     * @param value the value
     * @return a Mono that will complete when the message is sent
     */
    public Mono<Void> send(String topic, JsonNode key, JsonNode value) {
        return Mono.create(sink -> {
            final ProducerRecord<JsonNode, JsonNode> record = new ProducerRecord<>(topic, key, value);

            producer.get()
                    .send(record, (metadata, exception) -> {
                        if (exception != null) {
                            log.error("Error sending message to topic: {}", topic, exception);
                            sink.error(exception);
                        } else {
                            sink.success();
                        }
                    });
        });
    }

    @Override
    public void close() {
        if (producer.isInitialized()) {
            producer.get().close();
        }
    }
}