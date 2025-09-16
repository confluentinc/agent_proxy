package io.confluent.pas.agent.proxy.registration;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.pas.agent.common.services.*;
import io.confluent.pas.agent.common.services.schemas.Registration;
import io.confluent.pas.agent.common.services.schemas.RegistrationKey;
import io.confluent.pas.agent.proxy.registration.events.DeletedRegistrationEvent;
import io.confluent.pas.agent.proxy.registration.events.NewRegistrationEvent;
import io.confluent.pas.agent.proxy.registration.handlers.CompositeHandler;
import io.confluent.pas.agent.proxy.rest.a2a.A2AAsyncServer;
import io.confluent.pas.agent.proxy.rest.agents.AgentAsyncServer;
import io.modelcontextprotocol.server.McpAsyncServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The RegistrationCoordinator is the central component responsible for managing
 * tool registrations in a thread-safe and concurrent manner.
 * <p>
 * It coordinates the following processes:
 * - Listening for new registrations on the registration topic
 * - Processing incoming registrations and un-registrations concurrently
 * - Creating and managing handlers for each registered tool
 * - Maintaining the lifecycle of registrations with proper synchronization
 * - Broadcasting registration events to other components
 * - Handling registration conflicts and updates atomically
 * <p>
 * This class acts as the bridge between the Kafka-based registration system and
 * the protocol-specific servers (MCP, REST, A2A) that handle client communications.
 * It ensures thread-safe operations when managing multiple concurrent registrations
 * and unregistrations.
 */
@Slf4j
@Component
public class RegistrationCoordinator implements DisposableBean {

    /**
     * MCP server for handling Model Context Protocol communications asynchronously
     */
    @Getter
    private final McpAsyncServer mcpServer;

    /**
     * Agent-to-Agent async server for handling inter-agent communications
     */
    @Getter
    private final A2AAsyncServer a2AAsyncServer;

    /**
     * REST server for handling synchronous HTTP communications with agents
     */
    @Getter
    private final AgentAsyncServer restServer;

    /**
     * Handler for processing request/response pairs
     */
    @Getter
    private final RequestResponseHandler requestResponseHandler;

    /**
     * Thread-safe map of registration handlers indexed by registration name.
     * Uses ConcurrentHashMap to ensure atomic operations and visibility across threads
     * when adding, removing, or accessing handlers.
     */
    private final Map<String, CompositeHandler> handlers = new ConcurrentHashMap<>();

    /**
     * Client for interacting with the Schema Registry
     */
    private final SchemaRegistryClient schemaRegistryClient;

    /**
     * Service for managing registrations in Kafka
     */
    private final RegistrationService<RegistrationKey, Registration> registrationService;

    /**
     * Publisher for broadcasting registration events
     */
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Primary constructor used by Spring for dependency injection.
     * Initializes the coordinator with required dependencies and sets up the
     * registration service.
     *
     * @param kafkaConfiguration        Kafka configuration properties
     * @param requestResponseHandler    Handler for processing requests and
     *                                  responses
     * @param mcpServer                 MCP protocol server instance
     * @param restServer                REST protocol server instance
     * @param applicationEventPublisher Spring event publisher for broadcasting
     *                                  events
     */
    @Autowired
    public RegistrationCoordinator(KafkaConfiguration kafkaConfiguration,
                                   RequestResponseHandler requestResponseHandler,
                                   McpAsyncServer mcpServer,
                                   AgentAsyncServer restServer,
                                   A2AAsyncServer a2aAsyncServer,
                                   ApplicationEventPublisher applicationEventPublisher) {
        // Create a registration handler that will forward registration events to our
        // onRegistration method
        // This avoids the circular reference issue during construction
        CacheHandler.Handler<RegistrationKey, Registration> registrationHandler = registrations -> {
            if (registrations != null) {
                onRegistration(registrations);
            }
        };

        this.requestResponseHandler = requestResponseHandler;
        this.mcpServer = mcpServer;
        this.restServer = restServer;
        this.a2AAsyncServer = a2aAsyncServer;
        this.schemaRegistryClient = KafkaPropertiesFactory.getSchemRegistryClient(kafkaConfiguration);
        this.applicationEventPublisher = applicationEventPublisher;
        this.registrationService = new RegistrationService<>(
                kafkaConfiguration,
                RegistrationKey.class,
                Registration.class,
                registrationHandler);
    }

    /**
     * Secondary constructor primarily used for testing.
     * Allows injecting mock services for unit testing.
     *
     * @param requestResponseHandler    Handler for processing requests and
     *                                  responses
     * @param mcpServer                 MCP protocol server instance
     * @param restServer                REST protocol server instance
     * @param schemaRegistryClient      Client for interacting with Schema Registry
     * @param registrationService       Pre-configured registration service
     * @param applicationEventPublisher Spring event publisher for broadcasting
     *                                  events
     */
    public RegistrationCoordinator(RequestResponseHandler requestResponseHandler,
                                   McpAsyncServer mcpServer,
                                   AgentAsyncServer restServer,
                                   A2AAsyncServer a2aAsyncServer,
                                   SchemaRegistryClient schemaRegistryClient,
                                   RegistrationService<RegistrationKey, Registration> registrationService,
                                   ApplicationEventPublisher applicationEventPublisher) {
        this.requestResponseHandler = requestResponseHandler;
        this.mcpServer = mcpServer;
        this.restServer = restServer;
        this.a2AAsyncServer = a2aAsyncServer;
        this.schemaRegistryClient = schemaRegistryClient;
        this.registrationService = registrationService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Checks if a tool with the specified name is currently registered.
     *
     * @param name The name of the tool to check
     * @return true if the tool is registered and has an active handler, false
     * otherwise
     */
    public boolean isRegistered(String name) {
        return handlers.containsKey(name);
    }

    /**
     * Returns a list of all currently active registration handlers.
     * This provides access to all the tools currently registered with the system.
     *
     * @return List of all active CompositeHandler instances
     */
    public List<CompositeHandler> getAllRegistrationHandlers() {
        return new ArrayList<>(handlers.values());
    }

    /**
     * Registers a new tool with the system.
     * This will publish the registration to Kafka, which will then trigger
     * the registration process via the onRegistration callback.
     *
     * @param registration The registration details for the tool
     */
    public void register(Registration registration) {
        registrationService.register(new RegistrationKey(registration.getName()), registration);
    }

    /**
     * Unregisters a tool from the system.
     * This will remove the registration from Kafka, which will then trigger
     * the unregistration process via the onRegistration callback.
     *
     * @param name The name of the tool to unregister
     */
    public void unregister(String name) {
        registrationService.unregister(new RegistrationKey(name));
    }

    /**
     * Callback method invoked by the RegistrationService when registrations change.
     * This method is the entry point for processing registration changes from
     * Kafka.
     * <p>
     * It first updates the request/response handler with the new registrations,
     * then processes each registration individually.
     *
     * @param registrations Map of registration keys to their corresponding
     *                      registrations
     */
    void onRegistration(Map<RegistrationKey, Registration> registrations) {
        // Update the request/response handler with all registrations
        requestResponseHandler.addRegistrations(registrations.values());

        // Process each registration individually
        for (Map.Entry<RegistrationKey, Registration> entry : registrations.entrySet()) {
            handleRegistration(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Processes a single registration or unregistration event.
     * <p>
     * A null registration value indicates an unregistration event.
     * Otherwise, it handles the registration as a create or update operation.
     *
     * @param key          The registration key containing the tool name
     * @param registration The registration details, or null if unregistering
     */
    private void handleRegistration(RegistrationKey key, Registration registration) {
        final String registrationName = key.getName();

        // Handle unregistration
        if (registration == null) {
            if (handlers.containsKey(registrationName)) {
                unregisterHandler(registrationName);
            }
            return;
        }

        // Handle registration update or creation
        if (handlers.containsKey(registrationName)) {
            log.info("Registration already exists, updating: {}", registrationName);
            // First, unregister the existing handler to clean up resources
            unregisterHandler(registrationName);
        } else {
            log.info("Received new registration: {}", registrationName);
        }

        // Create a new handler for the registration
        createHandler(registration, registrationName)
                .doOnError(e -> log.error("Error creating handler for registration: {}", registrationName, e))
                .block();
    }

    /**
     * Creates and initializes a new handler for a registration.
     * <p>
     * This method constructs a CompositeHandler, initializes it, and handles
     * the success or failure of the initialization process.
     *
     * @param registration     The registration details
     * @param registrationName The name of the registration
     */
    private Mono<Void> createHandler(Registration registration, String registrationName) {
        try {
            // Create a new composite handler for the registration
            final CompositeHandler handler = new CompositeHandler(
                    registration,
                    requestResponseHandler,
                    schemaRegistryClient,
                    this);

            // Initialize the handler asynchronously and block until complete
            return handler.initialize()
                    .doOnSuccess(v -> handleSuccessfulRegistration(registrationName, handler, registration))
                    .doOnError(e -> handleFailedRegistration(registrationName, e));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    /**
     * Handles the successful initialization of a registration handler.
     * <p>
     * This method adds the handler to the active handlers map and publishes
     * a new registration event to notify other components.
     *
     * @param registrationName The name of the registration
     * @param handler          The initialized handler
     * @param registration     The registration details
     */
    private void handleSuccessfulRegistration(String registrationName, CompositeHandler handler,
                                              Registration registration) {
        log.info("Added registration: {}", registrationName);
        // Store the handler for future reference
        handlers.put(registrationName, handler);
        // Notify other components about the new registration
        applicationEventPublisher.publishEvent(new NewRegistrationEvent(this, registration));
    }

    /**
     * Handles a failed registration initialization.
     * <p>
     * This method logs the error and ensures the handler is removed from
     * the active handlers map if it was partially added.
     *
     * @param registrationName The name of the registration
     * @param error            The error that caused the failure
     */
    private void handleFailedRegistration(String registrationName, Throwable error) {
        log.error("Error adding registration: {}", registrationName, error);
        // Ensure the handler is removed if it was partially added
        handlers.remove(registrationName);
    }

    /**
     * Unregisters a handler by its registration name.
     * <p>
     * This method locates the handler, tears it down, and handles the
     * success or failure of the teardown process.
     *
     * @param registrationName The name of the registration to unregister
     */
    private void unregisterHandler(String registrationName) {
        log.info("Unregistering {}", registrationName);

        final CompositeHandler handler = handlers.get(registrationName);
        if (handler == null) {
            log.warn("No handler with name {}", registrationName);
            return;
        }

        // Tear down the handler asynchronously and block until complete
        handler.teardown()
                .doOnSuccess(v -> completeUnregistration(registrationName, handler))
                .doOnError(e -> log.error("Error unregistering {}", registrationName, e))
                .block();
    }

    /**
     * Completes the unregistration process after successful handler teardown.
     * <p>
     * This method removes the handler from the active handlers map and publishes
     * a deleted registration event to notify other components.
     *
     * @param registrationName The name of the registration
     * @param handler          The handler that was torn down
     */
    private void completeUnregistration(String registrationName, CompositeHandler handler) {
        log.info("Unregistered {}", registrationName);
        // Remove the handler from the active handlers
        handlers.remove(registrationName);
        // Notify other components about the deleted registration
        applicationEventPublisher.publishEvent(new DeletedRegistrationEvent(this, handler.getRegistration()));
    }

    /**
     * Cleans up resources when the Spring container is shutting down.
     * Implemented from DisposableBean interface.
     */
    @Override
    public void destroy() {
        // Close the registration service to release Kafka resources
        registrationService.close();
    }
}
