package io.confluent.pas.mcp.demo.commands.aicommands;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;
import io.confluent.pas.mcp.demo.commands.Assistant;
import io.confluent.pas.mcp.demo.commands.McpConnections;
import io.confluent.pas.mcp.demo.mcp.tools.McpToolProvider;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

public abstract class AbstractLLMCommands {

    protected final Terminal terminal;
    private final McpConnections connections;
    private final LineReader lineReader;
    private Assistant assistant;

    protected AbstractLLMCommands(Terminal terminal,
                                  LineReader lineReader,
                                  McpConnections connections) {
        this.terminal = terminal;
        this.lineReader = lineReader;
        this.connections = connections;
    }
    
    /**
     * Start a conversation with an AI
     *
     * @param model        Model to use for the conversation
     * @param systemPrompt System prompt to use for the conversation
     */
    protected void onChatStarted(ChatLanguageModel model, final String systemPrompt) {
        terminal.writer().println("Starting conversation with OpenAI model " + model + "...");
        terminal.writer().flush();

        // Process all MCP
        final ToolProvider toolProvider = new McpToolProvider(connections.getConnections());

        final ChatMemory chatMemory = MessageWindowChatMemory.withMaxMessages(20);
        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .chatMemory(chatMemory)
                .systemMessageProvider((val) -> systemPrompt)
                .toolProvider(toolProvider)
                .build();

        conversationLoop();
    }

    /**
     * The conversation loop
     */
    protected void conversationLoop() {
        terminal.writer().println("Conversation started");
        terminal.writer().flush();

        String line = lineReader.readLine("User: ");
        while (!line.equals("exit") && !line.equals("quit")) {
            final String result = assistant.chat(line).trim();
            terminal.writer().println("Assistant: " + result);
            terminal.writer().flush();

            line = lineReader.readLine("User: ");
        }

        assistant = null;
        terminal.writer().println("Conversation ended");
    }
}
