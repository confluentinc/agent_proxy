package io.confluent.pas.agent.proxy.registration;

import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.registration.schemas.RegistrationSchemas;
import io.modelcontextprotocol.server.McpAsyncServer;
import reactor.core.publisher.Mono;

import javax.naming.OperationNotSupportedException;

/**
 * Interface for handling registration of tools and resources.
 */
public interface RegistrationHandler<REQ, RES> {

    /**
     * Gets the registration for the tool or resource.
     *
     * @return the registration
     */
    Schemas.Registration getRegistration();

    /**
     * Gets the registration schemas for the tool or resource.
     *
     * @return the registration schemas
     */
    RegistrationSchemas getSchemas();

    /**
     * Registers the tool or resource with the specified server.
     *
     * @param mcpServer the server to register with
     * @return a Mono that completes when the registration is complete
     * @throws OperationNotSupportedException if the registration operation is not supported
     */
    Mono<Void> register(McpAsyncServer mcpServer) throws OperationNotSupportedException;

    /**
     * Unregisters the tool or resource from the specified server.
     *
     * @param mcpServer the server to unregister from
     * @return a Mono that completes when the un-registration is complete
     */
    Mono<Void> unregister(McpAsyncServer mcpServer);

    /**
     * Sends a request to the tool or resource.
     *
     * @param arguments the arguments to send
     * @return a Mono emitting the response as a map
     */
    Mono<RES> sendRequest(REQ arguments);
}