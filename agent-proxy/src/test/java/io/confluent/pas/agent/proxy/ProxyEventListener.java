package io.confluent.pas.agent.proxy;

import io.confluent.pas.agent.proxy.registration.events.NewRegistrationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Component
public class ProxyEventListener {

    private CountDownLatch latch = new CountDownLatch(1);

    public void reset() {
        latch = new CountDownLatch(1);
    }

    public void waitForEvent() throws InterruptedException {
        latch.await();
    }

    @Async()
    @EventListener(NewRegistrationEvent.class)
    public void onApplicationEvent(NewRegistrationEvent event) {
        latch.countDown();
    }
}
