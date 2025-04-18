package io.confluent.pas.agent.proxy.rest;

import io.confluent.pas.agent.common.services.Schemas;
import io.confluent.pas.agent.proxy.registration.RegistrationCoordinator;
import io.confluent.pas.agent.proxy.registration.RegistrationHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
public class ControlAPIController {

    private final RegistrationCoordinator coordinator;

    public ControlAPIController(RegistrationCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @GetMapping("/control/registrations")
    public List<Schemas.Registration> getRegistrations() {
        return coordinator
                .getAllRegistrationHandlers()
                .stream()
                .map(RegistrationHandler::getRegistration)
                .toList();
    }

    @PostMapping("/control/registration")
    public void register(Schemas.Registration registration) {
        if (coordinator.isRegistered(registration.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Registration with name %s already exists", registration.getName())
            );
        }

        coordinator.register(registration);
    }

    @PatchMapping("/control/registration")
    public void update(Schemas.Registration registration) {
        if (coordinator.isRegistered(registration.getName())) {
            coordinator.register(registration);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Registration with name %s not found", registration.getName())
            );
        }
    }

    @DeleteMapping("/control/registration/{name}")
    public void unregister(@PathVariable("name") String name) {
        if (!coordinator.isRegistered(name)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    String.format("Registration with name %s not found", name)
            );
        }

        coordinator.unregister(name);
    }
}
