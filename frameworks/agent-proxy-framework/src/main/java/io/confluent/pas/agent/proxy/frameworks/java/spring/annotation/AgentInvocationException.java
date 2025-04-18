package io.confluent.pas.agent.proxy.frameworks.java.spring.annotation;

public class AgentInvocationException extends RuntimeException {
    public AgentInvocationException(String message, Throwable cause) {
        super(message, cause);
    }
}