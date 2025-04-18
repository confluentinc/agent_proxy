# Example: LLM Accessing Client Information

[Back to Main README](../README.md)

## Table of Contents
- [Overview](#overview)
- [Building the Proxy and the Shell](#building-the-proxy-and-the-shell)
    - [Prerequisites](#prerequisites)
    - [Clone the Repository](#clone-the-repository)
    - [Build the Project](#build-the-project)
- [Tables and Queries](#tables-and-queries)
    - [Clients Table](#clients-table)
    - [Sample Data](#sample-data)
    - [Response Table](#response-table)
    - [Request Table](#request-table)
    - [Join Query](#join-query)
    - [Service Registration Query](#service-registration-query)
- [Demo Execution](#demo-execution)
    - [Step 1: Export Environment Variables](#step-1-export-environment-variables)
    - [Step 2: Open Multiple Terminals](#step-2-open-multiple-terminals)
        - [Terminal 1: Run the Agent Proxy](#terminal-1-run-the-mcpopenapi-proxy)
        - [Terminal 2: Run the MCP Shell](#terminal-2-run-the-mcp-shell)
    - [Step 3: Execute Commands in the MCP Shell](#step-3-execute-commands-in-the-mcp-shell)
- [Execution Example](#execution-example)

## Overview
This example demonstrates how an LLM can retrieve client information based on a user's first name. The process involves joining incoming requests with client data stored in a Flink table to generate responses.

## Building the Proxy and the Shell
To build the Agent proxy and the shell, follow these steps:

### Prerequisites
- Java 21+
- Maven

### Clone the Repository
```sh
git clone https://...
cd mcp-openapi-proxy
```

### Build the Project
```sh
mvn clean install
```

## Tables and Queries

You can execute the following SQL queries using either the **Confluent Cloud CLI** or the **Flink SQL workspace UI**. These tools provide an easy way to create tables, insert data, and manage streaming queries efficiently.

### Clients Table
Stores client details including their unique `client_id`, `firstname`, and `lastname`.

```sql
CREATE TABLE
    clients (
        client_id STRING PRIMARY KEY NOT ENFORCED,
        firstname STRING,
        lastname STRING
    ) DISTRIBUTED INTO 1 BUCKETS
WITH
    (
        'changelog.mode' = 'append',
        'value.fields-include' = 'all',
        'key.format' = 'json-registry',
        'value.format' = 'json-registry',
        'kafka.consumer.isolation-level' = 'read-uncommitted'
    );
```

#### Sample Data
```sql
INSERT INTO `clients` VALUES 
  ('123', 'Pascal', 'Vantrepote'),
  ('1123', 'Bob', 'Dylan'),
  ('567', 'Helen', 'Joker'),
  ('123', 'Jean Claude', 'Johns');
```

### Response Table
Stores the results of client information queries.

```sql
CREATE TABLE
    client_info_response (
        requestId STRING PRIMARY KEY NOT ENFORCED,
        lastname STRING,
        firstname STRING,
        client_id STRING
    ) DISTRIBUTED INTO 1 BUCKETS
WITH
    (
        'changelog.mode' = 'append',
        'value.fields-include' = 'except-key',
        'key.format' = 'json-registry',
        'value.format' = 'json-registry',
        'kafka.consumer.isolation-level' = 'read-uncommitted'
    );
```

### Request Table
Receives client information queries from LLMs.

```sql
CREATE TABLE
    client_info_request (
        requestId STRING PRIMARY KEY NOT ENFORCED,
        firstname STRING
    ) DISTRIBUTED INTO 1 BUCKETS
WITH
    (
        'changelog.mode' = 'append',
        'value.fields-include' = 'except-key',
        'key.format' = 'json-registry',
        'value.format' = 'json-registry',
        'kafka.consumer.isolation-level' = 'read-uncommitted'
    );
```

### Join Query
Processes client requests by joining them with stored client data to generate responses.

```sql
INSERT INTO client_info_response (requestId, `firstname`, lastname, client_id)
    SELECT client_info_request.requestId, clients.firstname, clients.lastname, clients.client_id FROM clients
    INNER JOIN client_info_request ON client_info_request.firstname = clients.firstname;
``` 

### Service Registration Query
Registers the client information retrieval service in the MCP/OpenAPI registry.

```sql
CREATE TABLE `_agent_registry` (
  name STRING PRIMARY KEY NOT ENFORCED,
  `description` STRING,
  requestTopicName STRING,
  responseTopicName STRING,
  correlationIdFieldName STRING
) DISTRIBUTED INTO 1 BUCKETS
WITH
    (
        'changelog.mode' = 'append',
        'kafka.cleanup-policy' = 'compact',
        'value.fields-include' = 'all',
        'key.format' = 'json-registry',
        'value.format' = 'json-registry',
        'kafka.consumer.isolation-level' = 'read-uncommitted'
    );
```

```sql
INSERT INTO `_agent_registry` VALUES ('clientInfo', 'Get the client information by first name', 'client_info_request', 'client_info_response', 'requestId');
```

## Demo Execution
To run the demo, you need to set up the necessary environment variables and open multiple terminal sessions.

### Step 1: Export Environment Variables
Before starting the proxy and shell, set the required environment variables:
```sh
export BROKER_URL="your-confluent-broker"
export JAAS_USERNAME="your-api-key"
export JAAS_PASSWORD="your-api-secret"
export SR_URL="your-schema-registry-url"
export SR_API_KEY="your-schema-registry-key"
export SR_API_SECRET="your-schema-registry-secret"
```

### Step 2: Open Multiple Terminals
#### Terminal 1: Run the Agent Proxy
```sh
java -jar proxy/target/proxy-0.0.1-SNAPSHOT.jar
```

#### Terminal 2: Run the MCP Shell
```sh
java -jar shell/target/shell-0.0.1-SNAPSHOT.jar
```

### Step 3: Execute Commands in the MCP Shell
Once the shell is running, execute the following commands to set up the service and test LLM integration:
```sh
mcp add sse http://localhost:8080
mcp list tools "MCP Server"
llm gemini
```
### Execution Example
Below is an example of how the shell interacts with the MCP proxy and LLM:

```
Connected to server: Confluent MCP Proxy (1.0.0)
shell:>mcp list tools "Confluent MCP Proxy"
--------------------
Tools for server: Confluent MCP Proxy
....................
Name: findLastName
Description: Find the last name of a given user
--------------------
Name: clientInfo
Description: Get the client information by first name
--------------------
shell:>llm gemini
Starting conversation with Gemini model dev.langchain4j.model.googleai.GoogleAiGeminiChatModel@510da778...
Conversation started
User: Hello my name is Helen
Assistant: Okay, I have your client information. Your first name is Helen, your client ID is 567, and your last name is Joker. Is there anything else I can help you with?
User: How about if I were Bob?
Assistant: Okay, I have the client information for Bob. Bob's last name is Dylan and Bob's client ID is 1123.
User: exit
Conversation ended
shell:>exit
```
