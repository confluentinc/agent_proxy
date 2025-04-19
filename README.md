# Agent Proxy for Event-Driven Agentic Choreography

## Overview

This open-source project provides an **Agent proxy** that enables **event-driven agentic choreography**
architecture. The project consists of two main modules:

1. **[Proxy Module](proxy/README.md)** – Implements an MCP-compliant and OpenAPI-compliant service that exposes
   Confluent Cloud topics.
2. **[Shell Module](shell/README.md)** – Enables interaction with MCP services and Large Language Models (LLMs) such
   as OpenAI, Gemini, and AWS Bedrock.
3. **[Agentic Choreography Framework](frameworks/agent-proxy-framework/README.md)** – A Java framework designed to accelerate the development of agents by taking care
   of infrastructure tasks like topic creation, schema registration, agent registration, and request/response handling.
   For more details, see the [Agentic Choreography Framework](frameworks/java/README.md).

## Examples

1. **[Client Info Example](examples/client_info.md)** - Demonstrates how an LLM can retrieve client information based on
   a user's first name.
2. **[JavaAgent Example](examples/JavaAgent/README.md)** – Demonstrates how a Java-based agent can integrate with the
   Agent Proxy using the Agentic Choreography Framework.
3. **[FlinkAgent Example](examples/FlinkAgent/README.md)** – Demonstrates how to build an agent with Apache Flink that
   registers its capabilities with the proxy.

## Key Features

- **MCP Protocol Support**: Allows **LLM tool discovery** and tool execution via **stdio or HTTP SSE**.
- **Confluent/Kafka Integration**: Provides seamless **real-time data access** via topics.
- **OpenAPI Compliance**: Standardized API definitions for easy integration.
- **Multi-LLM Support**: CLI module interacts with **OpenAI, Gemini, and AWS Bedrock**.

## Architecture

### **How It Works**

1. **Agent Registration**:
    - Each agent registers its capabilities with the **Agent Proxy**, allowing it to expose its services dynamically.
    - The proxy maintains a registry of available agent capabilities, making them discoverable for execution.

2. **Service Invocation**:
    - Once registered, agents can be **invoked via the MCP Proxy** using the **MCP protocol**.
    - Requests can be sent over **stdio or HTTP SSE**, enabling synchronous and asynchronous interactions.

3. **Event-Driven Execution**:
    - The **Agent Proxy** uses **Kafka topics as an API layer** to enable **real-time data flow**.
    - Agents can **subscribe to relevant topics**, process data, and publish results back into topics, ensuring
      a fully **event-driven architecture**

### **Diagram**

```

   +----------------------+          +---------------------------+
   |       Agent 1        | <------> |        Agent Proxy        |
   +----------------------+          +---------------------------+
                                              |        ^
                                              v        |
   +----------------------+          +---------------------------+
   |       Agent 2        | <------> |          Topics           |
   +----------------------+          +---------------------------+
```

### **Key Benefits**

- **Dynamic Agent Discovery**: Agents register their capabilities with the proxy, allowing them to be easily called when
  needed.
- **Real-Time Event Processing**: Topics enables scalable and reliable event-driven communication.
- **Multi-Agent Collaboration**: Multiple agents can work together, exchanging data and insights through the **Agent Proxy
  **.

This architecture provides a **flexible and scalable** foundation for **agentic workflows** by combining **MCP, OpenAPI,
and Topics**.

## Getting Started

### Prerequisites

- Java 21+
- Maven
- Confluent Cloud account (with Kafka topics configured)

### Installation

Clone the repository:

```sh
 git clone https://....
 cd mcp-openapi-proxy
```

Build the project:

```sh
mvn clean install
```

### Setting Up the Environment

```sh
# follow the prompts
./set_env.sh
```

### Running the Proxy

```sh
source .env # load environment variables set by set_env.sh
java -jar proxy/target/proxy-0.0.1-SNAPSHOT.jar
```

### Using the SHELL

```sh
source .env # load environment variables set by set_env.sh
java -jar shell/target/shell-0.0.1-SNAPSHOT.jar
```

## Configuration

### Required Environment Variables

To run the proxy, set the following environment variables:

- `BROKER_URL` - The Confluent Cloud broker URL
- `JAAS_USERNAME` - The JAAS username for authentication
- `JAAS_PASSWORD` - The JAAS password for authentication
- `SR_URL` - Schema Registry URL
- `SR_API_KEY` - Schema Registry API key
- `SR_API_SECRET` - Schema Registry API secret

### Optional Environment Variable

- `REGISTRY_TOPIC` - The topic for MCP registry (default: `_agent_registry`)
- `CLIENT_ID` - The unique client id. If not specified, a new UUID will be generated and saved into a `.pid` file in the current directory.

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## [License](LICENSE)
