package io.confluent.pas.agent.proxy.frameworks.java.spring.chat;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.ai.chat.messages.AssistantMessage.*;

public class MessageFactory {

    @SuppressWarnings("unchecked")
    public static UserMessage getUserMessage(Map<String, Object> userMessage) {
        final String text = userMessage.get("text").toString();
        final Map<String, Object> metadata = (Map<String, Object>) userMessage.get("metadata");

        return new UserMessage(text, new ArrayList<>(), metadata);
    }

    @SuppressWarnings("unchecked")
    public static AssistantMessage getAssistantMessage(Map<String, Object> assistantMessage) {
        final String text = assistantMessage.get("text").toString();
        final Map<String, Object> metadata = (Map<String, Object>) assistantMessage.get("metadata");
        final List<ToolCall> toolCallList = (List<ToolCall>) assistantMessage.get("toolCalls");

        return new AssistantMessage(text, metadata, toolCallList, new ArrayList<>());
    }

    public static SystemMessage getSystemMessage(Map<String, Object> systemMessage) {
        final String text = systemMessage.get("text").toString();
        return new SystemMessage(text);
    }

    @SuppressWarnings("unchecked")
    public static ToolResponseMessage getToolResponseMessage(Map<String, Object> toolResponseMessage) {
        final Map<String, Object> metadata = (Map<String, Object>) toolResponseMessage.get("metadata");
        final List<ToolResponseMessage.ToolResponse> toolResponses = (List<ToolResponseMessage.ToolResponse>) toolResponseMessage.get("responses");

        return new ToolResponseMessage(toolResponses, metadata);
    }

}
