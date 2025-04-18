package io.confluent.pas.agent.proxy.frameworks.java;

/**
 * Response class that holds the key and the response object
 *
 * @param <K>   Key type
 * @param <RES> Response type
 */
public record Response<K, RES>(K key, RES response) {
}
