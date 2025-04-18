package io.confluent.pas.mcp.demo.commands.aicommands;


import dev.langchain4j.model.bedrock.BedrockChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.confluent.pas.mcp.demo.commands.McpConnections;
import io.confluent.pas.mcp.demo.utils.ValueUtils;
import org.apache.commons.lang3.StringUtils;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Command(command = "llm", description = "Conversation with an AI")
public class BedrockCommands extends AbstractLLMCommands {

    public BedrockCommands(@Autowired Terminal terminal,
                           @Autowired @Lazy LineReader lineReader,
                           @Autowired McpConnections connections) {
        super(terminal, lineReader, connections);
    }

    @Command(command = "bedrock", group = "LLM", description = "Start a conversation with a Bedrock model")
    public void startConversation(@Option(required = true,
                                          longNames = "model",
                                          shortNames = 'm',
                                          defaultValue = "anthropic.claude-3-haiku-20240307-v1:0",
                                          description = "Model to use for the conversation") String model,
                                  @Option(longNames = "access-Key",
                                          shortNames = 'a',
                                          description = "Access key to use for the conversation") String accessKey,
                                  @Option(longNames = "secret-access-key",
                                          shortNames = 's',
                                          description = "Secret access key to use for the conversation") String secretAccessKey,
                                  @Option(longNames = "env-access-key",
                                          shortNames = 'k',
                                          defaultValue = "AWS_ACCESS_KEY_ID",
                                          description = "Environment variable name to use for the Access key") String envAccessKeyName,
                                  @Option(longNames = "env-secret-access",
                                          shortNames = 'e',
                                          defaultValue = "AWS_SECRET_ACCESS_KEY",
                                          description = "Environment variable name to use for the Access secret key") String envSecretName,
                                  @Option(longNames = "env-session-token",
                                          shortNames = 'o',
                                          defaultValue = "AWS_SESSION_TOKEN",
                                          description = "Environment variable name to use for the Session token") String envSessionToken,
                                  @Option(longNames = "session-token",
                                          shortNames = 't',
                                          description = "Session token") String sessionToken,
                                  @Option(longNames = "region",
                                          shortNames = 'r',
                                          defaultValue = "us-east-1",
                                          description = "Model region") String region,
                                  @Option(longNames = "prompt",
                                          shortNames = 'p',
                                          description = "System prompt") String systemPrompt) {
        final String apiKeyValue = ValueUtils.resolveValueOrEnv(accessKey, envAccessKeyName);
        final String apiSecretValue = ValueUtils.resolveValueOrEnv(secretAccessKey, envSecretName);
        final String token = ValueUtils.resolveValueOrEnv(sessionToken, envSessionToken);
        if (StringUtils.isEmpty(apiKeyValue) ||
                StringUtils.isEmpty(apiSecretValue) ||
                StringUtils.isEmpty(token)) {
            terminal.writer().println("API key, secret and session token are required.");
            return;
        }

        ChatLanguageModel languageModel = BedrockChatModel.builder()
                .modelId(model)
                .region(Region.of(region))
                .client(BedrockRuntimeClient
                        .builder()
                        .credentialsProvider(() -> AwsSessionCredentials.create(apiKeyValue, apiSecretValue, token))
                        .build()
                )
                .build();

        onChatStarted(languageModel, systemPrompt);
    }
}
