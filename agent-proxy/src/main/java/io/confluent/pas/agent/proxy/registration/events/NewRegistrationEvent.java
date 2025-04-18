package io.confluent.pas.agent.proxy.registration.events;

import io.confluent.pas.agent.common.services.Schemas;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event for new registration.
 */
@Getter
public class NewRegistrationEvent extends ApplicationEvent {

    private final Schemas.Registration registration;

    public NewRegistrationEvent(Object source, Schemas.Registration registration) {
        super(source);
        this.registration = registration;
    }

}
