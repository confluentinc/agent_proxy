# MCP Client Proxy

[Back to Main README](../README.md)

A Java-based proxy that exposes MCP (Model Context Protocol) servers as Kafka streaming applications. This tool bridges
the gap between MCP servers and Kafka. It allows agents to discover and utilize tools and resources over Kafka.

## Overview

The MCP Client Proxy is a command-line tool designed to facilitate interaction between MCP servers and Kafka. It
performs the following key functions:

1. Connects to an MCP server using Standard IO.
2. Exposes the MCP server's functionality as a Kafka streaming application.
3. Automatically registers the tool with the Agent Proxy for agent discovery.

This proxy enables seamless integration between MCP-compatible tools and Kafka-based systems. It facilitates
communication between various components in your architecture.

## Features

- **MCP Server Connectivity**: Establishes a connection to MCP servers.
- **Kafka Integration**: Transforms MCP functionalities into Kafka streaming applications.
- **Automatic Tool Registration**: Registers tools with the Agent Proxy for easy discovery by agents.
- **Configuration Flexibility**: Supports configuration through environment variables and YAML files.

## Installation

MCP Client Proxy is a standard Java application built with Maven.

### Prerequisites

- **Java 17 or higher**
- **Maven 3.6 or higher**

### Building from Source

1. **Clone the repository**:
   ```
   git clone https://github.com/confluentinc/mcp-openapi-proxy.git
   cd mcp-openapi-proxy
   ```

2. **Build with Maven**:
   ```
   mvn clean package
   ```

3. **Locate the JAR file**: The build process will generate a JAR file in the `frameworks/mpc-client-proxy/target`
   directory.

### Using the JAR

You can run the application directly from the JAR file:

```
java -jar ./frameworks/mpc-client-proxy/target/mcp-client-proxy-<version>.jar -c ./cfg.yaml
```

## Usage

```
mcp-client-proxy [options] [command]
```

### Commands

- `help`: Displays usage information and available commands.
- `-c, --config <path>`: Specifies the MCP server configuration file.

### Options

- `--spring.config.location=<path>`: Override the default proxy configuration file location.

### Example Usage

```
mcp-client-proxy -c ./cfg.yaml
```

This command starts the proxy using the MCP server configuration file `cfg.yaml` in the current directory. The proxy
then establishes a connection to the MCP server and exposes its functionalities through Kafka topics.

## Configuration

The MCP Client Proxy has two types of configuration:

### 1. Proxy Configuration

This configuration controls the proxy's behavior, Kafka connectivity, and logging. It can be set through:

- **Environment Variables** (recommended for production/deployment)
- **Configuration File** (can be specified with `--spring.config.location`)

#### Standard Proxy Configuration File

```yaml
spring:
  application:
    name: "MCPProxyClient"
  main:
    banner-mode: off
    web-application-type: none
kafka:
  application-id: "${APPLICATION_ID}"
  broker-servers: ${BROKER_URL}
  jaas-config: org.apache.kafka.common.security.plain.PlainLoginModule required username="${JAAS_USERNAME}" password="${JAAS_PASSWORD}";
  sr-url: ${SR_URL}
  sr-basic-auth: ${SR_API_KEY}:${SR_API_SECRET}
logging:
  file:
    name: ${LOG_FILE}
  pattern:
    console: ""
```

### 2. MCP Server Configuration

The MCP server details are specified in a separate configuration file that is passed to the proxy with the `-c` or
`--config` option.

#### Configuration File Format

The configuration file uses YAML format with the following structure:

```yaml
# Server execution configuration
command: "<command to execute>"
arguments:
  - "<arg1>"
  - "<arg2>"
  # ...more arguments as needed

# Tool registration information
tool:
  name: "<tool_name>"
  request_topic: "<request_topic_name>"   # Optional, defaults to "<tool_name>_request"
  response_topic: "<response_topic_name>" # Optional, defaults to "<tool_name>_response"
  output_schema: "<JSON schema string>"
  # Additional mcpTool properties as needed
```

### Example: Configuration for Python MCP Server

The following example shows how to configure the proxy to connect to a Python-based MCP server:

```yaml
command: "uv"
arguments:
  - "run"
  - "--with"
  - "mcp[cli]"
  - "mcp"
  - "run"
  - "/Users/pascal/projects/oss/mcp-openapi-proxy/examples/python/server.py"
tool:
  name: "add"
  request_topic: "override_request_topic"   # Optional, defaults to "<tool_name>_request"
  response_topic: "override_response_topic" # Optional, defaults to "<tool_name>_response"
  output_schema: "
{
   \"properties\":{
      \"result\":{
         \"connect.index\":0,
         \"type\":\"number\"
      }
   },
   \"required\":[
      \"result\"
   ],
   \"title\":\"Record\",
   \"type\":\"object\"
}
"
```

This configuration:

1. Uses the `uv` command to run a Python MCP server.
2. Registers a tool named "add" with the specified output schema.
3. Uses stdio mode for communication with the server.

## Troubleshooting

### Common Issues and Solutions

- **Issue**: Unable to connect to the MCP server.
    - **Solution**: Check the MCP server URL and ensure it is running.

- **Issue**: Kafka topics are not created.
    - **Solution**: Verify Kafka broker settings and ensure the application has the necessary permissions.

- **Issue**: Configuration file not found.
    - **Solution**: Ensure the path to the configuration file is correct and accessible.

## Kafka Integration

The MCP Client Proxy creates the necessary Kafka topics and configurations to expose the MCP server's functionality.

### Message Flow

1. The MCP server receives requests from the `request_topic`.
2. It processes the requests and sends responses to the `response_topic`.
3. Agents can discover and utilize these tools through the Kafka streaming interface, facilitating communication.

## Tool Registration

The proxy automatically registers the configured tool with the Agent Proxy. This enables discovery by agents. The
registration includes:

- Tool name
- Tool description
- Input/output schemas

## Contributing

Contributions are welcome! To submit issues or pull requests:

1. **Open an Issue**: Describe the problem or suggestion clearly.
2. **Submit a Pull Request**: Ensure your code adheres to existing style guidelines and includes tests where applicable.

Feel free to engage with the community and contribute to the project!

## [License](LICENSE)