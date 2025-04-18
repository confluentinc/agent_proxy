# Sentiment Analysis Agent Application

[Back to Main README](../../README.md)

## Overview

This sample application demonstrates an agent specialized in sentiment analysis built on the Java Agent
Framework. It emphasizes the use of the `@Agent` annotation, clearly defining Kafka request/response topics and
request/response classes for straightforward integration.

## Architecture

The architecture leverages annotations for automatic agent registration and simplified Kafka interactions:

```
+----------------------+          +---------------------------+          +------------------------------+
|   Incoming Request   | -------> |       Agent Proxy         | <------- | Kafka Response Topic         |
|   (MCP Client)       |          | (Routes via @Agent info)  |          | sentiment-analysis-responses |
+----------------------+          +---------------------------+          +------------------------------+
                                                 |                                      ^
                                                 v                                      |
                              +-----------------------------+        +--------------------------+
                              | Kafka Request Topic         | ------>| Sentiment Analysis Agent |
                              | sentiment-analysis-requests |        | (@Agent annotated class) |
                              +-----------------------------+        +--------------------------+
```

## Main Class (`AgentInstance.java`)

The main agent logic is encapsulated within the `AgentInstance.java` class. This class is explicitly annotated with
`@Agent` for integration into the Agent Proxy.

### `@Agent` Annotation Explained

```java
@Agent(
        name = "sample_java_agent",
        description = "A sample Java agent specialized in sentiment analysis",
        requestTopic = "sentiment-analysis-requests",
        responseTopic = "sentiment-analysis-responses",
        requestClass = Request.class,
        responseClass = SentimentResponse.class
)
```

- **name**: Uniquely identifies the agent instance.
- **description**: Clarifies the agent's purpose.
- **requestTopic**: Kafka topic subscribed by the agent for receiving requests.
- **responseTopic**: Kafka topic used to publish sentiment analysis results.
- **requestClass**: The structure used for incoming requests.
- **responseClass**: The class used for structured responses.

## Request/Response Classes

**Request.java**

```java
public class Request {
    private String query;

    // Getter and Setter
}
```

**SentimentResponse.java**

```java
public class SentimentResponse {
    private String sentiment;

    public SentimentResponse(String sentiment) {
        this.sentiment = sentiment;
    }

    // Getter and Setter
}
```

## Kafka Topics

- **Request Topic**: `sentiment-analysis-requests`  
  Consumed by the agent to perform sentiment analysis.

- **Response Topic**: `sentiment-analysis-responses`  
  Used for publishing analysis results.

## Demo Execution

To test the agent, follow these steps:

### Step 1: Start the Agent Proxy

```sh
java -jar proxy/target/proxy-0.0.1-SNAPSHOT.jar
```

### Step 2: Start the Java Sentiment Agent

```sh
java -jar exemples/JavaAgent/target/JavaAgent-0.0.1-SNAPSHOT.jar
```

### Step 3: Send Sample Requests

Send sentiment requests via the MCP shell:

```sh
mcp add sse http://localhost:8080
mcp list tools "MCP Server"
llm gemini
```

### Example Interaction in MCP Shell

```sh
shell:> mcp list tools "Confluent MCP Proxy"
--------------------
Tools for server: Confluent MCP Proxy
....................
Name: JavaSentimentAgent
Description: This agent analyzes sentiment in human queries.
--------------------
shell:> llm gemini
Starting conversation with Gemini model...
Conversation started
User: Hello, I am feeling happy today!
Assistant: Your sentiment is Positive.
User: exit
Conversation ended
shell:> exit
```

## Summary

By clearly defining the agent’s configuration through the `@Agent` annotation, the framework simplifies Kafka
interactions, enabling efficient sentiment analysis request handling within an MCP/OpenAPI ecosystem.

## Conclusion

This sample illustrates how a **Java-based Agent** can be exposed as an **MCP/OpenAPI service**, enabling seamless
integration with **MCP/OpenAPI clients**. By leveraging the **Agent Proxy**, real-time sentiment analysis can be
efficiently integrated into a broader ecosystem.

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

This project is licensed under the MIT License.
