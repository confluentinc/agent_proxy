package io.confluent.pas.mcp.demo.commands;

/**
 * Interface for an assistant that can chat with a user.
 */
public interface Assistant {
    String chat(String userMessage);
}
