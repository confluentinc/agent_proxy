package io.confluent.pas.mcp.proxy.frameworks.client.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.confluent.pas.mcp.proxy.frameworks.client.exceptions.ConfigurationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Getter
@Setter
public class AgentConfiguration {

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ToolConfiguration {
        @JsonProperty("name")
        private String name;
        @JsonProperty("output_schema")
        private JsonSchema output_schema;
        @JsonProperty(value = "request_topic", defaultValue = "")
        private String request_topic;
        @JsonProperty(value = "response_topic", defaultValue = "")
        private String response_topic;

        public String getRequest_topic() {
            if (request_topic == null || request_topic.isEmpty()) {
                return name + "_request";
            }

            return request_topic;
        }

        public String getResponse_topic() {
            if (response_topic == null || response_topic.isEmpty()) {
                return name + "_response";
            }

            return response_topic;
        }

        public void setOutput_schema(String output_schema) {
            this.output_schema = new JsonSchema(output_schema);
        }

        public void setOutput_schema(JsonSchema output_schema) {
            this.output_schema = output_schema;
        }
    }

    @JsonProperty("command")
    private String command;

    @JsonProperty("arguments")
    private List<String> arguments;

    @JsonProperty("mcpTool")
    @Getter
    private List<ToolConfiguration> tools;

    /**
     * Parses the agent configuration from a file.
     *
     * @param filePath The path to the configuration file.
     * @return The parsed agent configuration.
     * @throws ConfigurationException If there is an issue parsing the configuration.
     */
    public static AgentConfiguration fromFile(String filePath) throws ConfigurationException {
        if (!StringUtils.hasText(filePath)) {
            throw new ConfigurationException("File path cannot be empty");
        }

        final File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new ConfigurationException("File does not exist: " + filePath);
        }

        final String extension = filePath.substring(filePath.lastIndexOf('.')).toLowerCase();
        if (extension.equals(".yaml") || extension.equals(".yml")) {
            return parseYamlConfiguration(file, filePath);
        } else if (extension.equals(".json")) {
            return parseJsonConfiguration(file, filePath);
        } else {
            throw new ConfigurationException("Unsupported configuration file format: " + filePath);
        }
    }

    /**
     * Parses the agent configuration from a JSON file.
     *
     * @param file     The configuration file.
     * @param filePath The path to the configuration file.
     * @return The parsed agent configuration.
     * @throws ConfigurationException If there is an issue parsing the configuration.
     */
    private static AgentConfiguration parseJsonConfiguration(File file, String filePath) throws ConfigurationException {
        try {
            return JsonUtils.toObject(file, AgentConfiguration.class);
        } catch (IOException e) {
            throw new ConfigurationException("Error parsing configuration file: " + filePath, e);
        }
    }

    /**
     * Parses the agent configuration from a YAML file.
     *
     * @param file     The configuration file.
     * @param filePath The path to the configuration file.
     * @return The parsed agent configuration.
     * @throws ConfigurationException If there is an issue parsing the configuration.
     */
    private static AgentConfiguration parseYamlConfiguration(File file, String filePath) throws ConfigurationException {
        Yaml yaml = new Yaml();
        try {
            return yaml.loadAs(new FileInputStream(file), AgentConfiguration.class);
        } catch (Exception e) {
            throw new ConfigurationException("Error parsing configuration file: " + filePath, e);
        }
    }
}
