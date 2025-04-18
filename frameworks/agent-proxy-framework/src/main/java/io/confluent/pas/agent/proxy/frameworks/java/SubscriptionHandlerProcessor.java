package io.confluent.pas.agent.proxy.frameworks.java;

import io.confluent.pas.agent.proxy.frameworks.java.models.Key;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;

public class SubscriptionHandlerProcessor<K extends Key, REQ, RES> implements Processor<K, REQ, K, RES> {

    private final SubscriptionHandler.RequestHandler<K, REQ, RES> subscriptionHandler;
    private ProcessorContext<K, RES> context;

    public SubscriptionHandlerProcessor(SubscriptionHandler.RequestHandler<K, REQ, RES> subscriptionHandler) {
        this.subscriptionHandler = subscriptionHandler;
    }

    @Override
    public void init(ProcessorContext<K, RES> context) {
        this.context = context;
    }

    @Override
    public void process(Record<K, REQ> record) {
        final Request<K, REQ, RES> request = new Request<>(
                record.key(),
                record.value(),
                this::sendResponse);

        subscriptionHandler.onRequest(request);
    }

    void sendResponse(Response<K, RES> response) {
        context.forward(new Record<>(
                response.key(),
                response.response(),
                System.currentTimeMillis()));
    }
}
