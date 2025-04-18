package io.confluent.pas.mcp.exemple;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import io.confluent.pas.agent.proxy.frameworks.java.Request;
import io.confluent.pas.agent.proxy.frameworks.java.spring.annotation.Agent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Agent class responsible for handling sentiment analysis requests.
 */
@Slf4j
@Component
public class AgentInstance {

    private final Assistant assistant;

    /**
     * Constructor to initialize the AgentInstance with the necessary configuration.
     *
     * @param geminiKey    The API key for the Google AI Gemini model.
     * @param systemPrompt The system prompt to be used by the assistant.
     */
    public AgentInstance(@Value("${model.gemini-key}") String geminiKey,
                         @Value("${model.system-prompt}") String systemPrompt) {
        // Create a ChatLanguageModel using the Google AI Gemini model
        final ChatLanguageModel chatLanguageModel = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName("gemini-2.0-flash")
                .build();

        // Build the assistant using the AI services
        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .systemMessageProvider((val) -> systemPrompt)
                .build();
    }

    /**
     * Handles incoming requests by processing the query and responding with the sentiment analysis result.
     *
     * @param request The incoming request containing the query.
     */
    @Agent(
            name = "sample_java_agent",
            description = "A sample Java agent that performs sentiment analysis using the Google AI Gemini model.",
            request_topic = "sample_java_agent_request",
            response_topic = "sample_java_agent_response",
            requestClass = AgentQuery.class,
            responseClass = AgentResponse.class
    )
    public void onRequest(Request<Key, AgentQuery, AgentResponse> request) {
        log.info("Received request: {}", request.getRequest().query());

        // Process the query using the assistant and get the response
        final String response = assistant.chat(request.getRequest().query());

        // Respond to the request with the sentiment analysis result
        request.respond(new AgentResponse(response))
                .doOnError(e -> log.error("Failed to respond", e))
                .block();
    }
}