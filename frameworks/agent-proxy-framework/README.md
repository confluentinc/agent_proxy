# Java Agent Framework

[Back to Main README](../../README.md)

## Overview

This framework accelerates the development of **Agents** in Java. It simplifies agent lifecycle management
and Kafka topic interactions by using custom annotations like `@Agent` and `@Resource` for automatic registration and
handling.

## Features

- **`@Agent` Annotation**: Simplifies agent registration and request handling.
- **`@Resource` Annotation**: Automatically registers resources to the server and handles requests based on URI paths.
- **Spring Auto-Configuration**: Automatically configures beans based on dependencies and settings.
- **Kafka Integration**: Manages Kafka consumers and producers efficiently.
- **Topic Management**: Ensures necessary topics are created dynamically.

## Architecture

```
 +-------------------+        +---------------------------+        +---------------------------+        +-------------------+
 |  Incoming Request | -----> |       Agent Proxy         | -----> | Java Agent Framework      | -----> | Custom Java Agent |
 |   (MCP Client)    |        |  (Manages Agent Registry) |        |  (Handles Subscription)   |        |(Processes Request)|
 +-------------------+        +---------------------------+        +---------------------------+        +-------------------+
                                                                                   |
                                                                                   v
                                                                   +---------------------------+
                                                                   | Kafka Request/Response    |
                                                                   |  Topics Managed by Agent  |
                                                                   +---------------------------+
```

---

## Spring Auto-Configuration Support

This framework provides auto-configuration support for Spring-based applications, simplifying the setup by automatically
configuring beans for Kafka integration, agent registration, and subscription handling based on the configuration files.

### Minimal Configuration Example for Applications

Here’s an example of how an application using this framework can define a minimal `application.yml` configuration:

```yaml
spring:
  config:
    activate:
      on-profile: "default"
  application:
    name: "JavaAgent"
  main:
    web-application-type: none

kafka:
  application-id: "tool_agent"
  broker-servers: ${BROKER_URL}
  jaas-config: org.apache.kafka.common.security.plain.PlainLoginModule required username="${JAAS_USERNAME}" password="${JAAS_PASSWORD}";
  sr-url: ${SR_URL}
  sr-basic-auth: ${SR_API_KEY}:${SR_API_SECRET}
```

---

## Core Concepts: `@Agent` and `@Resource` Annotations

### `@Agent` Annotation

The `@Agent` annotation is the core feature for defining agents that automatically register and handle requests.

**Example Usage:**

```java

@Slf4j
@Component
public class AgentInstance {
    private final Assistant assistant;

    @Autowired
    public AgentInstance(@Value("${model.gemini-key}") String geminiKey,
                         @Value("${model.system-prompt}") String systemPrompt) {
        final ChatLanguageModel chatLanguageModel = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName("gemini-2.0-flash")
                .build();

        assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatLanguageModel)
                .systemMessageProvider((val) -> systemPrompt)
                .build();
    }

    @Agent(
            name = "sample_java_agent",
            description = "A sample Java agent that performs sentiment analysis using the Google AI Gemini model.",
            request_topic = "sample_java_agent_request",
            response_topic = "sample_java_agent_response",
            requestClass = AgentQuery.class,
            responseClass = AgentResponse.class
    )
    public void onRequest(Request<Key, AgentQuery, AgentResponse> request) {
        log.info("Received request: {}", request.getRequest().query());
        final String response = assistant.chat(request.getRequest().query());
        request.respond(new AgentResponse(response))
                .doOnError(e -> log.error("Failed to respond", e))
                .block();
    }
}
```

---

### `@Resource` Annotation

The `@Resource` annotation is used to automatically register resources to the server and handle incoming requests based
on specified URIs. Resources are delivered dynamically based on the URI patterns.

**Sample Application Delivering Resources:**

```java
/**
 * Agent class responsible for delivering resources.
 */
@Slf4j
@Component
public class ResourceAgent {
    private final static String URI = "client/{client_id}";
    private final static String MIME_TYPE = "application/json";

    private final UriTemplate template;

    /**
     * Constructor to initialize the ResourceAgent with required dependencies.
     */
    public ResourceAgent() {
        this.template = new UriTemplate(URI);
    }

    /**
     * Handles incoming requests by processing the query and responding with a message.
     *
     * @param request The incoming request containing the query.
     */
    @Resource(
            name = "resource-agent--rcs",
            description = "This agent returns resources.",
            request_topic = "resource-request",
            response_topic = "resource-response",
            contentType = MIME_TYPE,
            path = URI,
            responseClass = Schemas.TextResourceResponse.class
    )
    public void onRequest(Request<Key, Schemas.ResourceRequest, Schemas.TextResourceResponse> request) {
        log.info("Received request: {}", request.getRequest().getUri());

        // Extract values from the URI using the template
        final Map<String, Object> values = this.template.match(request.getRequest().getUri());

        // Respond to the request with a message containing the client_id
        request.respond(new Schemas.TextResourceResponse(
                        request.getRequest().getUri(),
                        MIME_TYPE,
                        "{ \"message\": \"Hello, " + values.get("client_id") + "!\" }"
                ))
                .doOnError(e -> log.error("Failed to respond to request", e))
                .block();
    }
}
```

---

### Key Features of `@Resource` Annotation

- **Automatic Registration:** Registers the resource with the specified properties at startup.
- **URI-Based Handling:** Dynamically handles requests based on URI patterns.
- **Method Binding:** Must be paired with a method that processes incoming requests.

---

## Registering Agents and Resources

- Use `@Agent` for registering agents that handle complex requests and responses via Kafka topics.
- Use `@Resource` for serving static or dynamic resources based on URI patterns.

---

## Conclusion

This framework streamlines the development of **MCP/OpenAPI Agents** in Java by providing:

- **`@Agent` Annotation** for agent registration and request handling.
- **`@Resource` Annotation** for dynamic resource delivery.
- **Spring Auto-Configuration** to reduce boilerplate code.
- **Efficient Kafka Integration** for seamless communication.

By leveraging this framework, developers can focus on business logic without worrying about configuration and
infrastructure.

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

This project is licensed under the MIT License.