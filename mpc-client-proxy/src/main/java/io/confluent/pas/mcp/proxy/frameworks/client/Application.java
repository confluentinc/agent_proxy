package io.confluent.pas.mcp.proxy.frameworks.client;

import io.confluent.pas.mcp.proxy.frameworks.client.internal.AgentConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        final Options options = new Options()
                .addOption("c", "config", true, "Configuration file")
                .addOption("h", "help", false, "Print this message");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (!cmd.hasOption("c")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("python-proxy", options);
                return;
            }

            // load the configuration file
            final AgentConfiguration agentConfiguration = AgentConfiguration.fromFile(cmd.getOptionValue("c"));

            // Create a new Spring application
            SpringApplication app = new SpringApplication(Application.class);

            // Register the agent configuration as a singleton bean
            app.addInitializers((applicationContext) -> applicationContext
                    .getBeanFactory()
                    .registerSingleton("agentConfiguration", agentConfiguration));

            // Run the application
            app.run(args);
        } catch (ParseException e) {
            log.error("Error parsing command line arguments", e);
        }
    }

}
