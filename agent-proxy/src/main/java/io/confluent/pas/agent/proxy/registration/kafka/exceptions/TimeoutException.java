package io.confluent.pas.agent.proxy.registration.kafka.exceptions;

/**
 * Exception thrown when a timeout occurs while waiting for a response from a
 * Kafka topic.
 */
public class TimeoutException extends Exception {

    public TimeoutException(String message) {
        super(message);
    }
}
