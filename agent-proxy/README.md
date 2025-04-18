# Proxy

[Back to Main README](../README.md)

## Table of Contents

- [Proxy](#proxy)
    - [Overview](#overview)
        - [Key Features](#key-features)
    - [Event-Driven Agent Choreography](#event-driven-agent-choreography)
        - [Key Characteristics](#key-characteristics)
        - [Example Workflow](#example-workflow)
    - [Communication Models](#communication-models)
        - [Tool Communication Model](#tool-communication-model)
        - [Resource Communication Model](#resource-communication-model)
        - [Common Patterns](#common-patterns)
        - [Protocol Access Methods](#protocol-access-methods)
    - [Schema Registry](#schema-registry)
        - [Key Functions](#key-functions)
        - [Tools Schema Definition](#tools-schema-definition)
    - [Configuration Example](#configuration-example)
        - [Resource Schema Definition](#resource-schema-definition)
    - [Tools Request/Response Schema Definition](#tools-request/response-schema-definition)
        - [Request/Response Schema](#request/response-schema)
        - [Request Schema](#request-schema)
        - [Response Schema](#response-schema)
    - [Resource Request/Response Schema Definition](#resource-request/response-schema-definition)
        - [Resource Types](#resource-types)
        - [Request Schema Definition](#request-schema-definition)
        - [Response Schema Structure](#response-schema-structure)
            - [Common Response Fields](#common-response-fields)
            - [Text Resource Response Schema](#text-resource-response-schema)
            - [Blob Resource Response Schema](#blob-resource-response-schema)
        - [Resource Handling Best Practices](#resource-handling-best-practices)
    - [Correlation ID for Request/Response Handling](#correlation-id-for-request/response-handling)
    - [Running the Proxy](#running-the-proxy)
        - [Prerequisites](#prerequisites)
        - [Installation](#installation)
        - [Starting the Proxy](#starting-the-proxy)
    - [Configuration](#configuration)
        - [Required Environment Variables](#required-environment-variables)
        - [Optional Environment Variable](#optional-environment-variable)
    - [Contributing](#contributing)
    - [License](#license)

## Overview

The **Proxy** service is designed to bridge Confluent Cloud topics with structured APIs by implementing the **MCP
protocol** and **OpenAPI**. It allows seamless and secure communication between tools, resources, and services through a
well-defined interface.

### Key Features

- **MCP Protocol Support:** Facilitates tool and resource discovery and execution via **stdio** or **HTTP SSE**.
- **OpenAPI Integration:** Provides RESTful access to tools and resources with Swagger UI support.
- **Schema Registry Support:** Ensures consistent data formats through Confluent's Schema Registry.
- **Correlation ID Tracking:** Supports request tracing across the entire system.
- **API Endpoints:**
    - Tools: `/api/{tool name}`
    - Resources: `/rcs/{resource uri}`

## Event-Driven Agent Choreography

The **Proxy** service enables **event-driven agent choreography** by coordinating multiple autonomous agents through
event streams in Kafka. Unlike traditional orchestration with a central controller, this model allows agents to make
independent decisions based on the events they receive.

### Key Characteristics

- **Event Propagation:** Events published to Kafka topics trigger agent actions.
- **Decentralized Decision-Making:** Agents subscribe to relevant topics and execute tasks autonomously.
- **Inter-Agent Communication:** Agents communicate indirectly via events, promoting loose coupling.
- **Dynamic Workflows:** Enables flexible and adaptive workflows without a central orchestrator.

### Example Workflow

1. **Event Publication:** A client request is converted into an event and published to a Kafka topic.
2. **Agent Reaction:** Agents subscribed to this topic process the event based on their roles.
3. **Response Propagation:** Processed results are published to response topics, triggering further actions if needed.

## Communication Models

The Proxy service implements two distinct communication patterns for interacting with tools and resources, both
leveraging Kafka as the underlying message transport system.

### Tool Communication Model

Tools follow a synchronous request-response pattern where:

1. **Client Request**: Client sends a request message to a specific tool via the proxy
2. **Topic Routing**: Proxy routes the message to the appropriate tool's request topic in Kafka
3. **Processing**: Tool consumes the message, processes the request, and generates a response
4. **Response Publication**: Tool publishes the response to its designated response topic
5. **Delivery**: Proxy consumes the response from the topic and delivers it back to the client

This model supports both:

- **Synchronous operations** via HTTP requests (with appropriate timeouts)
- **Streaming operations** via HTTP Server-Sent Events (SSE) for long-running tool executions

Key characteristics:

- **Active Processing**: Tools actively perform operations based on the request
- **Stateful Operations**: Many tool operations involve complex state changes
- **Correlation Tracking**: Each request-response pair is linked by a correlation ID

### Resource Communication Model

Resources follow a data-oriented retrieve/update pattern:

1. **Resource Request**: Client requests a specific resource via URI
2. **Topic Lookup**: Proxy determines which resource topic contains the requested resource
3. **Retrieval**: Proxy retrieves the resource content from the appropriate topic
4. **Format Conversion**: Proxy formats the resource according to its type (text or blob)
5. **Delivery**: Proxy returns the formatted resource to the client

Key characteristics:

- **Data-Centric**: Resources represent data objects rather than active operations
- **URI-Addressable**: Each resource has a unique URI
- **Type-Specific Handling**: Different handling for text vs. binary resources
- **MIME Type Integration**: Resources are associated with specific MIME types

### Common Patterns

Both communication models share these common patterns:

1. **Schema Validation**: All messages are validated against their respective schemas in the Schema Registry
2. **Correlation ID Tracking**: All requests and responses contain correlation IDs for traceability
3. **Topic-Based Routing**: Kafka topics are used for message distribution
4. **Registry-Based Discovery**: The registry topic maintains metadata about available tools and resources
5. **Event-Driven Architecture**: The system follows event-driven principles where messages trigger actions

### Protocol Access Methods

The proxy exposes both tools and resources through multiple protocols:

| Feature                | MCP (stdio/HTTP SSE) | OpenAPI/REST                     | 
|------------------------|----------------------|----------------------------------|
| Tool Invocation        | ✓                    | ✓ (via `/api/{tool name}`)       |
| Resource Retrieval     | ✓                    | ✓ (via `/rcs/{resource uri}`)    |
| Streaming Responses    | ✓                    | ✓ (via SSE)                      |
| Synchronous Operations | Limited              | ✓                                |
| Swagger Documentation  | N/A                  | ✓ (via `/swagger-ui/index.html`) |

## Schema Registry

The **Schema Registry** is a critical component that stores and manages schemas for all messages exchanged between tools
and resources. It ensures data compatibility and enables seamless communication between different agents.

### Key Functions

- **Schema Validation:** Enforces data format consistency by validating messages against registered schemas.
- **Version Control:** Maintains different versions of schemas to support forward and backward compatibility.
- **Centralized Management:** Provides a single source of truth for all schemas used in the system.

### Tools Schema Definition

This section defines the schema for configuring tools.

| Field Name             | Type   | Required | Default Value | Description                                                                              |
|------------------------|--------|----------|---------------|------------------------------------------------------------------------------------------|
| correlationIdFieldName | String |          | correlationId | Specifies the field name in the payload that contains the correlation ID, if applicable. |
| name                   | String | ✓        |               | A unique identifier for the service being registered.                                    |                                    
| description            | String | ✓        |               | A clear and concise human-readable description of the service's purpose.                 |
| requestTopicName       | String | ✓        |               | The Kafka topic on which the service listens for incoming requests.                      |
| responseTopicName      | string | ✓        |               | The Kafka topic to which the service sends its responses.                                |

```json

{
  "properties": {
    "correlationIdFieldName": {
      "type": "string"
    },
    "description": {
      "type": "string"
    },
    "name": {
      "type": "string"
    },
    "requestTopicName": {
      "type": "string"
    },
    "responseTopicName": {
      "type": "string"
    }
  },
  "required": [
    "name",
    "requestTopicName",
    "responseTopicName",
    "description"
  ],
  "title": "Record",
  "type": "object"
}
```

## Configuration Example

Here is an example configuration for a service using the above schema:

```json

{
  "name": "order-processing-service",
  "description": "Handles order processing and inventory updates",
  "requestTopicName": "orders.requests",
  "responseTopicName": "orders.responses",
  "correlationIdFieldName": "correlationId"
}
```

### Resource Schema Definition

This section defines the schema for configuring resources.

| Field Name             | Type   | Required | Default Value | Description                                                                              |
|------------------------|--------|----------|---------------|------------------------------------------------------------------------------------------|
| correlationIdFieldName | String |          | correlationId | Specifies the field name in the payload that contains the correlation ID, if applicable. |
| name                   | String | ✓        |               | A unique identifier for the service being registered.                                    |                                    
| description            | String | ✓        |               | A clear and concise human-readable description of the service's purpose.                 |
| requestTopicName       | String | ✓        |               | The Kafka topic on which the service listens for incoming requests.                      |
| responseTopicName      | string | ✓        |               | The Kafka topic to which the service sends its responses.                                |
| mimeType               | string | ✓        |               | The MIME type of the resource.                                                           |
| url                    | string | ✓        |               | The URL of the resource.                                                                 |

```json

{
  "properties": {
    "registrationType": {
      "connect.index": 5,
      "type": "string"
    },
    "correlationIdFieldName": {
      "connect.index": 4,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    },
    "description": {
      "connect.index": 1,
      "type": "string"
    },
    "name": {
      "connect.index": 0,
      "type": "string"
    },
    "requestTopicName": {
      "connect.index": 2,
      "type": "string"
    },
    "responseTopicName": {
      "connect.index": 3,
      "type": "string"
    },
    "mimeType": {
      "connect.index": 6,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    },
    "url": {
      "connect.index": 7,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    }
  },
  "required": [
    "name",
    "description",
    "registrationType",
    "requestTopicName",
    "responseTopicName"
  ],
  "additionalProperties": false,
  "title": "Record",
  "type": "object"
}
```

The registry schema is essential for managing resource discovery and execution within MCP/OpenAPI. The schema is stored
in
Confluent's Schema Registry and follows a structured format to ensure consistency across different resources and
services.
Resources can contain either text or binary data.

## Tools Request/Response Schema Definition

### Request/Response Schema

The proxy follows a **schema-first approach**, meaning it relies on Confluent's **Schema Registry** to validate request
and response messages before exposing them through MCP and OpenAPI. Currently, JSON schemas are used for defining and
enforcing data structures.

### Request Schema

Each request follows a structured schema stored in the Schema Registry. A valid request includes a **correlation ID**
for
tracking and a **payload** containing the requested operation.

### Response Schema

Responses must also conform to a registered schema, ensuring consistency and validation. The **correlation ID** must
match the request for proper tracking.

## Resource Request/Response Schema Definition

Resources can contain either text or binary data. The proxy handles both types through a structured schema approach
stored in Confluent's Schema Registry.

### Resource Types

Resources are classified into two main types:

- **Text Resources**: Used for retrieving text-based content (JSON, XML, Markdown, etc.)
- **Blob Resources**: Used for binary data (images, PDFs, executables, etc.)

### Request Schema Definition

Resource requests follow a simple URI-based schema. The client needs to specify which resource they want to access:

```json

{
  "properties": {
    "uri": {
      "connect.index": 0,
      "type": "string"
    }
  },
  "required": [
    "uri"
  ],
  "title": "Record",
  "type": "object"
}
```

Key components:

- **uri** (*string*, required): The unique identifier for the resource being requested

### Response Schema Structure

Responses for resources follow a consistent pattern with type-specific variations:

#### Common Response Fields

- **type** (*string*, required): Indicates whether the resource is "text" or "blob"
- **uri** (*string*, required): The resource identifier that matches the request
- **mimeType** (*string*, required): The MIME type of the resource (e.g., "application/json", "image/png")

#### Text Resource Response Schema

Text resource responses include the actual text content in the `text` field:

```json

{
  "properties": {
    "type": {
      "connect.index": 0,
      "enum": [
        "text",
        "blob"
      ],
      "default": "text",
      "type": "string"
    },
    "uri": {
      "connect.index": 1,
      "type": "string"
    },
    "mimeType": {
      "connect.index": 2,
      "type": "string"
    },
    "text": {
      "connect.index": 3,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    }
  },
  "required": [
    "type",
    "uri",
    "mimeType"
  ],
  "title": "Record",
  "type": "object"
}
```

Note that the `text` field can be null, which allows for resource existence checks without retrieving content.

#### Blob Resource Response Schema

Blob resource responses contain base64-encoded binary data in the `blob` field:

```json

{
  "properties": {
    "type": {
      "connect.index": 0,
      "enum": [
        "text",
        "blob"
      ],
      "default": "text",
      "type": "string"
    },
    "uri": {
      "connect.index": 1,
      "type": "string"
    },
    "mimeType": {
      "connect.index": 2,
      "type": "string"
    },
    "blob": {
      "connect.index": 3,
      "oneOf": [
        {
          "type": "null"
        },
        {
          "type": "string"
        }
      ]
    }
  },
  "required": [
    "type",
    "uri",
    "mimeType"
  ],
  "title": "Record",
  "type": "object"
}
```

### Resource Handling Best Practices

When working with resources through the proxy:

1. **Use appropriate MIME types**: Always specify the correct MIME type for resources to ensure proper handling by
   clients
2. **URI conventions**: Follow a consistent naming convention for resource URIs (e.g., hierarchical paths)

## Correlation ID for Request/Response Handling

To ensure proper tracking of requests and responses, every query must include a **correlation ID**. Instead of being
part of the event payload, the **correlation ID** is derived from the key of the event. This allows efficient message
routing and ensures that responses can be matched correctly with their corresponding requests.

Using the event key as a correlation ID ensures consistency across the system while leveraging Kafka's partitioning and
message ordering mechanisms effectively.

## Running the Proxy

### Prerequisites

- Java 21+
- Maven
- Confluent Cloud account (with Kafka topics and Schema Registry configured)

### Installation

Clone the repository:

```sh
git clone https://...
cd  cd mcp-openapi-proxy/proxy
```

Build the project:

```sh
mvn clean install
```

### Starting the Proxy

```sh
java -jar target/proxy-0.0.1-SNAPSHOT.jar
```

## Configuration

The proxy requires specific environment variables for correct operation:

### Required Environment Variables

- `BROKER_URL`   - Confluent Cloud broker URL
- `JAAS_USERNAME`   - Authentication username
- `JAAS_PASSWORD`   - Authentication password
- `SR_URL`   - Schema Registry URL
- `SR_API_KEY`   - Schema Registry API key
- `SR_API_SECRET`   - Schema Registry API secret

### Optional Environment Variable

- `REGISTRY_TOPIC`   - The topic for MCP registry (default: `_agent_registry`)
- `CLIENT_ID` - The unique client id. If not specified, a new UUID will be generated and saved into a `.pid` file in the
  current directory.

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## [License](../LICENSE)



