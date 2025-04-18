# Flink-Based Sentiment Analysis Sample

[Back to Main README](../../README.md)

## Overview

This sample demonstrates how to leverage **Confluent Flink** to create a real-time **sentiment analysis agent**. The
agent
processes incoming text data, analyzes sentiment, and outputs the results for downstream applications. The **Agent
Proxy** is used to expose the Flink agent as an **service**, allowing seamless interaction with clients.

![Agent Proxy Demo](./Demo.gif)

## Architecture

```
 +-------------------+        +----------------------------+        +-----------------+       +-------------------+
 |  Incoming Request | -----> |        Agent Proxy         | -----> |  User message   | ----->| Flink Sentiment   |
 |   (MCP Client)    |        |  (Exposes Flink Agent)     |        | (Kafka Topic)   |       | Analysis Agent    |
 +-------------------+        +----------------------------+        +-----------------+       +-------------------+
                                                                                                        |
                                                                                                        v
                                                                                        +-------------------------------+
                                                                                        |  Sentiment Analysis Results   |
                                                                                        | (Kafka Topic)                 |
                                                                                        +-------------------------------+
                                                                                                        |
                                                                                                        v
                                                                                        +----------------------------+
                                                                                        |        Agent Proxy         |
                                                                                        |     (Responds to Query)    |
                                                                                        +----------------------------+
```

## Flink Queries

### Step 1: Create Request and Response Tables

These tables handle incoming sentiment analysis queries and store the results.

```sql
CREATE TABLE
    user_message
(
    correlationId STRING PRIMARY KEY NOT ENFORCED,
    message       STRING
)
    WITH
        (
        'changelog.mode' = 'append',
        'kafka.cleanup-policy' = 'compact',
        'value.fields-include' = 'except-key',
        'key.format' = 'json-registry',
        'value.format' = 'json-registry',
        'kafka.consumer.isolation-level' = 'read-uncommitted'
        );
```

```sql
CREATE TABLE
    user_sentiment
(
    correlationId STRING PRIMARY KEY NOT ENFORCED,
    sentiment     STRING
)
    WITH
        (
        'changelog.mode' = 'append',
        'kafka.cleanup-policy' = 'compact',
        'value.fields-include' = 'except-key',
        'key.format' = 'json-registry',
        'value.format' = 'json-registry',
        'kafka.consumer.isolation-level' = 'read-uncommitted'
        );
```

### Step 2: Set Up Google AI Connection

Create a connection to **Google AI** for sentiment analysis.

```sh
confluent flink connection create googleai-cli-connection \
--cloud GCP \
--region us-east4\
--environment ${ENV_ID} \
--type googleai \
--endpoint https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-latest:generateContent \
--api-key ${GEMINI_API_KEY}
```

### Step 3: Create Sentiment Analysis Model

Define a **machine learning model** for sentiment classification.

```sql
CREATE
MODEL sentimentmodel
INPUT(text STRING)
OUTPUT(sentiment STRING)
COMMENT 'sentiment analysis model'
WITH (
  'provider' = 'googleai',
  'task' = 'classification',
  'googleai.connection' = 'googleai-cli-connection',
  'googleai.system_prompt' = 'You are specialized in sentiment analysis. Your job is to analyze the sentiment of the text and provide the sentiment score. The sentiment score is positive, negative, or neutral.'
);
```

### Step 4: Implement Sentiment Analysis Logic

Run a **LATERAL TABLE** join to analyze incoming text and store results.

```sql
INSERT INTO `user_sentiment`
SELECT `correlationId`, sentiment
FROM `user_message`,
     LATERAL TABLE (ML_PREDICT('sentimentmodel', message));
```

### Step 5: Expose the Flink Agent via Agent Proxy

To expose the Flink-based sentiment analysis agent to a **client**, register the agent in the **Agent Proxy**.

```sql
INSERT INTO `_agent_registry`
VALUES ('FlinkAgent', 'This agent returns sentiments of a human request.', 'user_message',
        'user_sentiment', 'correlationId');
```

This registration allows **MCP/OpenAPI Clients** to interact with the Flink agent as a structured API, enabling seamless
real-time sentiment analysis in an interoperable manner.

## Conclusion

This sample demonstrates how a **Flink model or job** can be exposed to other agents or applications via the **Agent Proxy**.
By integrating **Agent proxy**, the Flink Sentiment Analysis Agent can seamlessly interact with
external clients, making it easily accessible for various use cases that require real-time sentiment processing. This
approach enables interoperability and simplifies the integration of streaming analytics into broader AI workflows.

## Contributing

Contributions are welcome! Feel free to open issues and submit pull requests.

## License

This project is licensed under the MIT License.
