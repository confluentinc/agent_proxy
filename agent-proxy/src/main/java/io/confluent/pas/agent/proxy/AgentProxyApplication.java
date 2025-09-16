package io.confluent.pas.agent.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@EnableConfigurationProperties
public class AgentProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentProxyApplication.class, args);
    }

}