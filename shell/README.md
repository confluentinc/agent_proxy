# MCP Shell

[Back to Main README](../README.md)

## Overview

The **MCP Shell** is an interactive command-line application that allows users to interact with MCP servers. It provides
a shell-like interface for seamless communication with MCP services. It supports both **STDIO-based MCP servers** and *
*HTTP SSE-based MCP servers**. Additionally, it enables users to test tools using popular Large Language Models (LLMs)
such as **OpenAI, Gemini, and AWS Bedrock**.

The **MCP Shell** is a command-line application that allows users to interact with MCP servers. It supports both *
*STDIO-based MCP servers** and **HTTP SSE-based MCP servers**. Additionally, it enables users to test tools using
popular Large Language Models (LLMs) such as **OpenAI, Gemini, and AWS Bedrock**.

## Key Features

- **MCP Server Interaction**: List available tools from MCP servers.
- **Multi-Protocol Support**: Works with both **STDIO** and **HTTP SSE** MCP servers.
- **LLM Integration**: Test tools using OpenAI, Gemini, and AWS Bedrock.
- **Help Command**: Users can type `--help` to display usage information.

## Installation

### Prerequisites

- Java 21+
- Maven

### Build the Project

```sh
git clone https://...
cd mcp-openapi-proxy/shell
mvn clean install
```

## Usage

### Starting the Interactive Shell

To start the MCP Shell in interactive mode:

```sh
java -jar target/mcp-shell.jar
```

Once inside the shell, you can enter commands interactively.

### Help Command

The MCP Shell provides a set of built-in and custom commands to facilitate interaction with MCP servers and LLM models.
To display available commands inside the shell, use:

```sh
help
```

#### Available Commands

##### Built-In Commands

- `help`: Display help about available commands.
- `stacktrace`: Display the full stacktrace of the last error.
- `clear`: Clear the shell screen.
- `quit`, `exit`: Exit the shell.
- `history`: Display or save the history of previously run commands.
- `version`: Show version info.
- `script`: Read and execute commands from a file.

##### LLM Commands

When a conversation with an LLM is started, the user needs to type `quit` or `exit` to stop the conversation.
Additionally, users can get more information about a command by typing the command followed by `--help`.

- `llm openai`: Start a conversation with an OpenAI model.
- `llm bedrock`: Start a conversation with a Bedrock model.
- `llm gemini`: Start a conversation with a Gemini model.

##### MCP Commands

- `mcp add stdio`: Add an STDIO MCP Server.
- `mcp list`: List all MCP Servers.
- `mcp list tools`: List tools available on MCP Servers.
- `mcp add sse`: Add an SSE MCP Server.

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## [License](../LICENSE)
