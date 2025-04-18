package io.confluent.pas.agent.common.services;

import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RegistrationServiceHandlerTest {

    @Mock
    private RegistrationServiceHandler.Handler<Schemas.RegistrationKey, Schemas.Registration> handler;

    private RegistrationServiceHandler<Schemas.RegistrationKey, Schemas.Registration> registrationServiceHandler;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        registrationServiceHandler = new RegistrationServiceHandler<>(handler);
    }

    @Test
    public void testCacheInitializedWithNoEntries() {
        Map<TopicPartition, Long> checkpoints = new HashMap<>();
        registrationServiceHandler.cacheInitialized(0, checkpoints);

        assertTrue(registrationServiceHandler.isEmpty());
        verify(handler, never()).handleRegistrations(any());
    }

    @Test
    public void testCacheInitializedWithEntries() {
        Map<TopicPartition, Long> checkpoints = new HashMap<>();
        registrationServiceHandler.cacheInitialized(1, checkpoints);

        assertFalse(registrationServiceHandler.isEmpty());
        verify(handler, never()).handleRegistrations(any());
    }

    @Test
    public void testHandleUpdateBeforeInitialization() {
        Schemas.RegistrationKey key = new Schemas.RegistrationKey("key");
        Schemas.Registration value = new Schemas.Registration();
        TopicPartition tp = new TopicPartition("topic", 0);

        registrationServiceHandler.handleUpdate(key, value, null, tp, 0L, 0L);

        verify(handler, never()).handleRegistrations(any());
    }

    @Test
    public void testHandleUpdateAfterInitialization() {
        Schemas.RegistrationKey key = new Schemas.RegistrationKey("Key");
        Schemas.Registration value = new Schemas.Registration();
        TopicPartition tp = new TopicPartition("topic", 0);

        registrationServiceHandler.cacheInitialized(1, new HashMap<>());
        registrationServiceHandler.handleUpdate(key, value, null, tp, 0L, 0L);

        verify(handler, times(1)).handleRegistrations(Map.of(key, value));
    }

    @Test
    public void testHandleUpdateWithNullValue() {
        Schemas.RegistrationKey key = new Schemas.RegistrationKey("Name");
        TopicPartition tp = new TopicPartition("topic", 0);

        registrationServiceHandler.cacheInitialized(1, new HashMap<>());
        registrationServiceHandler.handleUpdate(key, null, new Schemas.Registration(), tp, 0L, 0L);

        verify(handler, times(1)).handleRegistrations(new HashMap<>() {{
            put(key, null);
        }});
    }
}