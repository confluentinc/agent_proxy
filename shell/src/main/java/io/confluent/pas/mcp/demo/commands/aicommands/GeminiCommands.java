package io.confluent.pas.mcp.demo.commands.aicommands;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import io.confluent.pas.mcp.demo.commands.McpConnections;
import io.confluent.pas.mcp.demo.utils.ValueUtils;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

@Command(command = "llm", description = "Conversation with an AI")
public class GeminiCommands extends AbstractLLMCommands {

    public GeminiCommands(@Autowired Terminal terminal,
                          @Autowired @Lazy LineReader lineReader,
                          @Autowired McpConnections connections) {
        super(terminal, lineReader, connections);
    }

    @Command(command = "gemini", group = "LLM", description = "Start a conversation with a Gemini model")
    public void startConversation(@Option(required = true,
                                          longNames = "model",
                                          shortNames = 'm',
                                          defaultValue = "gemini-2.0-flash",
                                          description = "Model to use for the conversation") String model,
                                  @Option(longNames = "api-key",
                                          shortNames = 'k',
                                          description = "API key to use for the conversation") String apiKey,
                                  @Option(longNames = "env",
                                          shortNames = 'e',
                                          defaultValue = "GEMINI_API_KEY",
                                          description = "Environment variable name to use for the API key") String envKeyName,
                                  @Option(longNames = "prompt",
                                          shortNames = 'p',
                                          description = "System prompt") String systemPrompt) {
        final String envApiKey = ValueUtils.resolveValueOrEnv(apiKey, envKeyName);
        if (StringUtils.isEmpty(envApiKey)) {
            terminal.writer().println("API key is required");
            terminal.writer().flush();
            return;
        }

        final ChatLanguageModel chatLanguageModel = GoogleAiGeminiChatModel.builder()
                .apiKey(envApiKey)
                .modelName(model)
                .build();

        onChatStarted(chatLanguageModel, systemPrompt);
    }
}
