package io.confluent.pas.agent.proxy.frameworks.java.spring.annotation;

import io.confluent.pas.agent.proxy.frameworks.java.models.Key;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark methods that act as MCP agents in a Spring application.
 * Methods annotated with @Agent will be automatically registered to handle Kafka-based messaging.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Agent {
    /**
     * The unique name identifier for this agent.
     */
    String name();

    /**
     * A description of what this agent does.
     */
    String description();

    /**
     * The Kafka topic name where requests will be received.
     */
    String request_topic();

    /**
     * The Kafka topic name where responses will be sent.
     */
    String response_topic();
    
    /**
     * The class type for the Kafka message key.
     * Must extend the Key class.
     * Defaults to Key.class.
     */
    Class<? extends Key> keyClass() default Key.class;

    /**
     * The class type for the request message payload.
     */
    Class<?> requestClass();

    /**
     * The class type for the response message payload.
     */
    Class<?> responseClass();
}