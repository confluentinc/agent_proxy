package io.confluent.pas.agent.proxy.frameworks.java;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * Request class that holds the key and the request object
 *
 * @param <K>   Key type
 * @param <REQ> Request type
 * @param <RES> Response type
 */
@Slf4j
@AllArgsConstructor
public class Request<K, REQ, RES> {
    @Getter
    private final K key;
    @Getter
    private final REQ request;
    private final Consumer<Response<K, RES>> responseConsumer;

    /**
     * Respond to the request
     *
     * @param response Response object
     */
    public Mono<Void> respond(Response<K, RES> response) {
        return Mono.create(sink -> {
            try {
                responseConsumer.accept(response);
                sink.success();
            } catch (Exception e) {
                log.error("Error responding to request", e);
                sink.error(e);
            }
        });
    }

    /**
     * Respond to the request
     *
     * @param response Response object
     */
    public Mono<Void> respond(RES response) {
        return this.respond(new Response<>(key, response));
    }
}
