# Resource Provider Agent Application

[Back to Main README](../../README.md)

## Overview

This sample application demonstrates a **Resource Provider Agent** built using the Java Agent Framework. It
focuses explicitly on the usage of the `@Resource` annotation to handle resource-related requests and responses via
Kafka topics.

---

## Architecture

The architecture leverages the annotation-based configuration for automatic integration and handling resource requests:

```
+----------------------+          +-----------------------------+          +-----------------------+
|   Incoming Request   | -------> |       Agent Proxy           | <------- | Kafka Response Topic  |
|   (MCP Client)       |          | (Routes via @Resource info) |          | resource-response     |
+----------------------+          +-----------------------------+          +-----------------------+
                                                 |                                      ^
                                                 v                                      |
                              +-----------------------------+        +-----------------------------+
                              | Kafka Request Topic         | ------>| Resource Provider Agent     |
                              | resource-request            |        | (@Resource annotated class) |
                              +-----------------------------+        +-----------------------------+
```

---

## Prerequisites

- Java 21+
- Maven
- Confluent Cloud account (with Kafka topics and Schema Registry configured)

## Installation

Clone the repository:

```sh
git clone https://...
cd mcp-openapi-proxy
```

Build the project:

```sh
mvn clean install
```

## Running the Agent

Start the Java-based resource delivery agent:

```sh
java -jar ./examples/JavaRCSProvider/target/JavaRCSProvider-0.0.1-SNAPSHOT.jar
```

---

## Main Class (`ResourceAgent.java`)

The core logic is implemented in the `ResourceAgent.java` class, explicitly marked with the `@Resource` annotation:

### `@Resource` Annotation Explained

```java
@Resource(
        name = "resource-agent--rcs",
        description = "This agent returns resources.",
        request_topic = "resource-request",
        response_topic = "resource-response",
        contentType = "application/json",
        path = "client/{client_id}",
        responseClass = Schemas.TextResourceResponse.class
)
```

- **name**: Unique identifier for the resource agent instance.
- **description**: Clarifies the agent's resource-provisioning purpose.
- **request_topic**: Kafka topic from which resource requests are consumed.
- **response_topic**: Kafka topic used to publish resource responses.
- **contentType**: MIME type (`application/json`) of the response content.
- **path**: URI template defining the structure of resource requests.
- **responseClass**: Class defining the structured response (`TextResourceResponse`).

---

## Request and Response Classes

The agent handles structured requests and generates structured responses:

### Request Class:

```java
public class ResourceRequest {
    private String uri;

    // Getter and Setter
}
```

### Response Class (`TextResourceResponse`):

```java
public class TextResourceResponse {
    private String uri;
    private String contentType;
    private String content;

    public TextResourceResponse(String uri, String contentType, String content) {
        this.uri = uri;
        this.contentType = contentType;
        this.content = content;
    }

    // Getters and Setters
}
```

---

## Kafka Topics

- **Request Topic**: `resource-request`  
  Topic subscribed by the agent to handle incoming resource requests.

- **Response Topic**: `resource-response`  
  Topic used for publishing resource responses back to requestors.

---

## Summary

The `@Resource` annotation efficiently configures the Resource Provider Agent, specifying clear communication channels (
Kafka topics) and structured data (request and response classes), ensuring straightforward integration within
MCP/OpenAPI-driven ecosystems.

---

## Demo Execution with MCP Inspector

To test the agent using the MCP Inspector, follow these steps:

### Step 1: Install and Start MCP Inspector

Run the following command:

```sh
npx mcp-inspector@latest
```

This command will open the MCP Inspector interface in your default web browser.

### Step 2: Connect to MCP Server

In the MCP Inspector interface:

- Enter the MCP server URL (e.g., `http://localhost:8080`).
- Click **Connect**.

### Step 3: List Available Resources

- Go to the **Resources** section in the MCP Inspector UI.
- View the list of available resources, including `JavaResourceAgent`.

### Step 4: Send Requests and Test

- Select the `/client/{client_id}` tool.
- Use the request form to send a sample request, such as:

```
Bob
```

- Submit the request and view the response directly in the interface.

### Example Output

```json
{
  "contents": [
    {
      "uri": "client/Bob",
      "mimeType": "application/json",
      "text": "{ \"message\": \"Hello, Bob!\" }"
    }
  ]
}
```

### Viewing Logs

- Navigate to the **History** section in the MCP Inspector UI to view real-time logs and responses.

## Conclusion

This sample illustrates how to use MCP Inspector's web interface for testing resource delivery alongside the MCP/OpenAPI
service.
