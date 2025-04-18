package io.confluent.pas.mcp.proxy.frameworks.client.internal;

import io.confluent.pas.agent.common.services.KafkaConfiguration;

public class KafkaToolConfiguration implements KafkaConfiguration {

    private final String clientId;
    private final String brokerServers;
    private final String schemaRegistryUrl;
    private final String applicationId;
    private final String saslJaasConfig;
    private final String schemaRegistryBasicAuthUserInfo;

    public KafkaToolConfiguration(KafkaConfiguration other, AgentConfiguration.ToolConfiguration tool) {
        this.clientId = other.clientId();
        this.brokerServers = other.brokerServers();
        this.schemaRegistryUrl = other.schemaRegistryUrl();
        this.applicationId = tool.getName() + "_" + other.applicationId();
        this.saslJaasConfig = other.saslJaasConfig();
        this.schemaRegistryBasicAuthUserInfo = other.schemaRegistryBasicAuthUserInfo();
    }

    @Override
    public String clientId() {
        return clientId;
    }

    @Override
    public String brokerServers() {
        return brokerServers;
    }

    @Override
    public String schemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    @Override
    public String applicationId() {
        return applicationId;
    }

    @Override
    public String saslJaasConfig() {
        return saslJaasConfig;
    }

    @Override
    public String schemaRegistryBasicAuthUserInfo() {
        return schemaRegistryBasicAuthUserInfo;
    }
}
