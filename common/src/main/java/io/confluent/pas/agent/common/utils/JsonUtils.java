package io.confluent.pas.agent.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    /**
     * Converts an object to another Object using JSON convertion.
     *
     * @param object the object to convert
     * @param clazz  the class of the object to convert to
     * @param <T>    the type of the object to convert to
     * @return an object of the specified class
     */
    public static <T> T toObject(Object object, Class<T> clazz) {
        return MAPPER.convertValue(object, clazz);
    }

    /**
     * Converts a JSON node to an object of the specified class.
     *
     * @param jsonNode the JSON node to convert
     * @param clazz    the class of the object to convert to
     * @param <T>      the type of the object to convert to
     * @return an object of the specified class
     */
    public static <T> T toObject(JsonNode jsonNode, Class<T> clazz) {
        return MAPPER.convertValue(jsonNode, clazz);
    }

    /**
     * Converts a JSON string to an object of the specified class.
     *
     * @param jsonString the JSON string to convert
     * @param clazz      the class of the object to convert to
     * @param <T>        the type of the object to convert to
     * @return an object of the specified class
     * @throws JsonProcessingException if an error occurs during deserialization
     */
    public static <T> T toObject(String jsonString, Class<T> clazz) throws JsonProcessingException {
        return MAPPER.readValue(jsonString, clazz);
    }

    /**
     * Converts a JSON file to an object of the specified class.
     *
     * @param file  the JSON file to convert
     * @param clazz the class of the object to convert to
     * @param <T>   the type of the object to convert to
     * @return an object of the specified class
     * @throws IOException if an error occurs during deserialization
     */
    public static <T> T toObject(File file, Class<T> clazz) throws IOException {
        return MAPPER.readValue(file, clazz);
    }

    /**
     * Converts a JsonNode to a Map<String, Object>.
     *
     * @param jsonNode the JsonNode to convert
     * @return a Map representation of the JsonNode
     */
    public static Map<String, Object> toMap(JsonNode jsonNode) {
        return MAPPER.convertValue(jsonNode, MAP_TYPE);
    }

    /**
     * Converts a JSON string to a Map<String, Object>.
     *
     * @param json the JSON string to convert
     * @return a Map representation of the JSON string
     * @throws JsonProcessingException if an error occurs during deserialization
     */
    public static Map<String, Object> toMap(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, MAP_TYPE);
    }

    /**
     * Converts an object to a Map<String, Object>.
     *
     * @param object the object to convert
     * @return a Map representation of the object
     */
    public static Map<String, Object> toMap(Object object) {
        return MAPPER.convertValue(object, MAP_TYPE);
    }

    /**
     * Converts a Map<String, Object> to a JsonNode.
     *
     * @param map the Map to convert
     * @return a JsonNode representation of the Map
     */
    public static JsonNode toJsonNode(Map<String, Object> map) {
        return MAPPER.valueToTree(map);
    }
    
    /**
     * Converts a JSON string to a JsonNode.
     *
     * @param json the JSON string to convert
     * @return a JsonNode representation of the JSON string
     * @throws JsonProcessingException if an error occurs during deserialization
     */
    public static JsonNode toJsonNode(String json) throws JsonProcessingException {
        return MAPPER.readTree(json);
    }

    /**
     * Converts an object to a JsonNode.
     *
     * @param object the object to convert
     * @return a JsonNode representation of the object
     */
    public static JsonNode toJsonNode(Object object) {
        return MAPPER.valueToTree(object);
    }

    /**
     * Converts an object to a JSON string.
     *
     * @param object the object to convert
     * @return a JSON string representation of the object
     * @throws JsonProcessingException if an error occurs during serialization
     */
    public static String toString(Object object) throws JsonProcessingException {
        return MAPPER.writeValueAsString(object);
    }

}