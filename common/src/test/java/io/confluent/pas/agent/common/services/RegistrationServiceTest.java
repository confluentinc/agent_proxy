package io.confluent.pas.agent.common.services;

import io.kcache.KafkaCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RegistrationServiceTest {

    @Mock
    private KafkaConfiguration kafkaConfiguration;

    @Mock
    private RegistrationServiceHandler.Handler<Schemas.RegistrationKey, Schemas.Registration> handler;

    @Mock
    private KafkaCache<Schemas.RegistrationKey, Schemas.Registration> registrationCache;

    private RegistrationService<Schemas.RegistrationKey, Schemas.Registration> registrationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        registrationService = new RegistrationService<>(registrationCache);
    }

    @Test
    public void testGetAllRegistrations() {
        List<Schemas.Registration> registrations = List.of(new Schemas.Registration());
        when(registrationCache.values()).thenReturn(registrations);

        List<Schemas.Registration> result = registrationService.getAllRegistrations();

        assertEquals(registrations, result);
    }

    @Test
    public void testIsRegistered() {
        Schemas.RegistrationKey key = new Schemas.RegistrationKey();
        when(registrationCache.get(key)).thenReturn(new Schemas.Registration());

        boolean result = registrationService.isRegistered(key);

        assertTrue(result);
    }

    @Test
    public void testRegister() {
        Schemas.RegistrationKey key = new Schemas.RegistrationKey();
        Schemas.Registration registration = new Schemas.Registration();

        registrationService.register(key, registration);

        verify(registrationCache, times(1)).put(key, registration);
    }

    @Test
    public void testUnregister() {
        Schemas.RegistrationKey key = new Schemas.RegistrationKey();

        registrationService.unregister(key);

        verify(registrationCache, times(1)).remove(key);
    }

    @Test
    public void testClose() throws IOException {
        registrationService.close();

        verify(registrationCache, times(1)).close();
    }
}