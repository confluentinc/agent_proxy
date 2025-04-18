package io.confluent.pas.mcp.exemple;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class JavaAgent {
    public static void main(String[] args) {
        SpringApplication.run(JavaAgent.class, args);
    }
}