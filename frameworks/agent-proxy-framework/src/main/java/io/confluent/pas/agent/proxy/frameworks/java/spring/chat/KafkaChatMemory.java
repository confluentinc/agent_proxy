package io.confluent.pas.agent.proxy.frameworks.java.spring.chat;

import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import io.confluent.kafka.serializers.KafkaJsonSerializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import io.confluent.pas.agent.common.services.KafkaConfiguration;
import io.confluent.pas.agent.common.services.KafkaPropertiesFactory;
import io.confluent.pas.agent.common.utils.JsonUtils;
import io.kcache.KafkaCache;
import io.kcache.KafkaCacheConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ChatMemory that uses Kafka as a persistent storage.
 * This class manages chat messages in a distributed manner using Kafka topics.
 */
public class KafkaChatMemory implements ChatMemory {

    /**
     * Internal representation of a chat message that can be serialized to Kafka.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class MemoryMessage extends HashMap<String, Object> {

        public static MemoryMessage fromMessage(Message message) {
            return JsonUtils.toObject(message, MemoryMessage.class);
        }

        public static Message toMessage(MemoryMessage message) {
            final String messageType = message.get("messageType").toString().toLowerCase();
            return switch (messageType) {
                case "user" -> MessageFactory.getUserMessage(message);
                case "system" -> MessageFactory.getSystemMessage(message);
                case "assistant" -> MessageFactory.getAssistantMessage(message);
                case "tool" -> MessageFactory.getToolResponseMessage(message);
                default -> throw new IllegalStateException("Unexpected value: " + messageType);
            };
        }

    }

    /**
     * Container class for storing a list of messages in Kafka.
     */
    @Getter
    @Setter
    public static class Memory extends ArrayList<MemoryMessage> {
    }

    /**
     * Kafka cache storing the chat memory data.
     */
    private final KafkaCache<String, Memory> memoryKafkaCache;

    /**
     * Constructor that initializes the Kafka cache with the specified configuration.
     *
     * @param memoryId         The identifier for this chat memory instance.
     * @param memoryProperties Kafka configuration properties.
     */
    public KafkaChatMemory(final String memoryId, final Map<String, ?> memoryProperties) {
        final Map<String, Object> srConfig = new HashMap<>(memoryProperties);
        srConfig.put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, Memory.class);

        final Serde<String> keySerdes = new Serdes.StringSerde();
        keySerdes.configure(memoryProperties, true);

        final Serde<Memory> valueSerdes = new Serdes.WrapperSerde<>(
                new KafkaJsonSerializer<>(),
                new KafkaJsonDeserializer<>()
        );
        valueSerdes.configure(srConfig, false);

        final Map<String, Object> kcacheProperties = configureKCacheProperties(memoryId, srConfig);
        this.memoryKafkaCache = initializeKafkaCache(kcacheProperties, keySerdes, valueSerdes);
    }

    public KafkaChatMemory(final String memoryId, final KafkaConfiguration configration) {
        this(memoryId, KafkaPropertiesFactory.getProducerProperties(configration)
                .entrySet()
                .stream()
                .map(enty -> Map.entry(enty.getKey().toString(), enty.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Adds a list of messages to the specified conversation.
     *
     * @param conversationId The unique identifier for the conversation.
     * @param messages       The list of messages to add.
     */
    @Override
    public void add(String conversationId, List<Message> messages) {
        final List<MemoryMessage> msg = messages.stream()
                .map(MemoryMessage::fromMessage)
                .toList();

        // Get the existing memory or create a new one
        final Memory memory = (memoryKafkaCache.containsKey(conversationId))
                ? memoryKafkaCache.get(conversationId)
                : new Memory();
        memory.addAll(msg);

        //
        memoryKafkaCache.put(conversationId, memory);
    }

    /**
     * Retrieves the last N messages from a conversation.
     *
     * @param conversationId The unique identifier for the conversation.
     * @param lastN          The number of most recent messages to retrieve.
     * @return List of messages, empty if conversation doesn't exist.
     */
    @Override
    public List<Message> get(String conversationId, int lastN) {
        final Memory memory = memoryKafkaCache.get(conversationId);
        if (memory != null) {
            return memory
                    .stream()
                    .skip(Math.max(0, memory.size() - lastN))
                    .map(MemoryMessage::toMessage)
                    .toList();
        }

        return List.of();
    }

    /**
     * Clears all messages for a specific conversation.
     *
     * @param conversationId The unique identifier for the conversation to clear.
     */
    @Override
    public void clear(String conversationId) {
        memoryKafkaCache.remove(conversationId);
    }

    private Map<String, Object> configureKCacheProperties(String memoryId, Map<String, Object> srConfig) {
        final Map<String, Object> kcacheProperties = srConfig.entrySet()
                .stream()
                .map(entry -> {
                    if (!entry.getKey().startsWith("kafkacache.")) {
                        return Map.entry("kafkacache." + entry.getKey(), entry.getValue());
                    } else {
                        return entry;
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        kcacheProperties.put(KafkaCacheConfig.KAFKACACHE_CLIENT_ID_CONFIG, memoryId);
        kcacheProperties.put(KafkaCacheConfig.KAFKACACHE_GROUP_ID_CONFIG, memoryId + "_grp");
        kcacheProperties.put(KafkaCacheConfig.KAFKACACHE_TOPIC_CONFIG, "_" + memoryId + "_memory");
        return kcacheProperties;
    }

    private KafkaCache<String, Memory> initializeKafkaCache(Map<String, Object> kcacheProperties,
                                                            Serde<String> keySerdes,
                                                            Serde<Memory> valueSerdes) {
        final KafkaCache<String, Memory> cache = new KafkaCache<>(
                new KafkaCacheConfig(kcacheProperties),
                keySerdes,
                valueSerdes,
                null,
                null);
        cache.init();
        return cache;
    }
}