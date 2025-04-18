package io.confluent.pas.mcp.demo.commands;

import io.confluent.pas.mcp.demo.mcp.McpServerConnection;
import io.confluent.pas.mcp.demo.mcp.connection.McpSseConnection;
import io.confluent.pas.mcp.demo.mcp.connection.McpStdioConnection;
import io.confluent.pas.mcp.demo.utils.ValueUtils;
import io.modelcontextprotocol.spec.McpSchema;
import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.util.List;

@Command(command = "mcp", group = "MCP", description = "Manage MCP Servers")
public class McpServerManagement {

    public enum ConnectionType {
        sse,
        stdio
    }

    private final Terminal terminal;
    private final McpConnections mcpConnections;

    public McpServerManagement(@Autowired Terminal terminal,
                               @Autowired McpConnections mcpConnections) {
        this.terminal = terminal;
        this.mcpConnections = mcpConnections;
    }

    /**
     * Add an STDIO MCP Server.
     *
     * @param command The command to run.
     * @param args    The command line arguments.
     */
    @Command(command = "add stdio", description = "Add an STDIO MCP Server")
    public void addStdioMcpServer(@Option(required = true,
                                          longNames = "command",
                                          shortNames = 'c',
                                          description = "Command to run") String command,
                                  @Option(arity = CommandRegistration.OptionArity.ONE_OR_MORE,
                                          longNames = "args",
                                          shortNames = 'a',
                                          description = "Command line arguments") String[] args) {
        addServer(new McpStdioConnection(command, List.of(args)));
    }

    /**
     * Add an SSE MCP Server.
     *
     * @param url The URL of the server.
     */
    @Command(command = "add sse", description = "Add an SSE MCP Server")
    public void addSseMcpServer(
            @Option(required = true,
                    longNames = "url",
                    shortNames = 'u',
                    description = "MCP Server URL") String url,
            @Option(longNames = "username",
                    shortNames = 'n',
                    description = "Username") String username,
            @Option(longNames = "password",
                    shortNames = 'p',
                    description = "Password") String password,
            @Option(longNames = "username_key",
                    defaultValue = "MCP_USERNAME",
                    description = "Username environment variable key name") String usernameKey,
            @Option(longNames = "password_key",
                    defaultValue = "MCP_PASSWORD",
                    description = "Password environment variable key name") String passwordKey) {
        final String resolvedUsername = ValueUtils.resolveValueOrEnv(username, usernameKey);
        if (resolvedUsername == null) {
            terminal.writer().println("Username is required");
            terminal.writer().flush();
            return;
        }

        final String resolvedPassword = ValueUtils.resolveValueOrEnv(password, passwordKey);
        if (resolvedPassword == null) {
            terminal.writer().println("Password is required");
            terminal.writer().flush();
            return;
        }

        addServer(new McpSseConnection(url, resolvedUsername, resolvedPassword));
    }

    /**
     * Add a server.
     *
     * @param connection The connection to the server.
     */
    private void addServer(McpServerConnection connection) {
        // Connect to the server
        connection.connect()
                .doOnError(throwable -> {
                    terminal.writer().println("Failed to connect to server: " + throwable.getMessage());
                })
                .doOnSuccess((Void) -> {
                    mcpConnections.addServer(connection.getServerName(), connection);
                    terminal.writer().println("Connected to server: " + connection.getServerName() + " (" + connection.getServerVersion() + ")");

                    // Dump the tools
                    listToolsMcpServers(connection.getServerName());
                })
                .block();
    }

    /**
     * List all MCP Servers.
     */
    @Command(command = "list", description = "List all MCP Servers")
    public void listMcpServers() {
        if (mcpConnections.isEmpty()) {
            terminal.writer().println("No MCP Servers found");
            return;

        }

        terminal.writer().println("MCP Servers:");
        mcpConnections.forEach((key, value) -> {
            terminal.writer().println("--------------------");
            terminal.writer().println("Name: " + key);
            terminal.writer().println("Version: " + value.getServerVersion());
            terminal.writer().println("--------------------");
        });
    }

    /**
     * List Tools for an MCP Servers.
     */
    @Command(command = "list tools", description = "List tools MCP Servers")
    public void listToolsMcpServers(@Option(required = true,
            description = "Name of the server") String serverName) {
        if (!mcpConnections.containsServer(serverName)) {
            terminal.writer().println("No MCP Servers found");
            return;

        }

        final McpServerConnection connection = mcpConnections.getServer(serverName);
        if (!connection.isToolSupported()) {
            terminal.writer().println("Server does not support tools");
            return;
        }

        terminal.writer().println("--------------------");
        terminal.writer().println("Tools for server: " + serverName);
        terminal.writer().println("....................");

        final List<McpSchema.Tool> tools = connection.getTools();
        tools.forEach(tool -> {
            terminal.writer().println("Name: " + tool.name());
            terminal.writer().println("Description: " + tool.description());
            terminal.writer().println("--------------------");
        });
    }
}
