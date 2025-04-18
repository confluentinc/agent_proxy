package io.confluent.pas.mcp.exemple;

/**
 * Interface for an assistant that can chat with a user.
 */
public interface Assistant {
    String chat(String userMessage);
}
