package com.marketplace.outbox;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OutboxEventHandlerRegistry {
    private final Map<OutboxEventType, OutboxEventHandler<?>> handlers;

    public OutboxEventHandlerRegistry(List<OutboxEventHandler<?>> handlers) {

        this.handlers = handlers.stream()
                        .collect(Collectors.toMap(
                                OutboxEventHandler::supports,
                                Function.identity()
                        ));
    }

    public OutboxEventHandler<?> get(OutboxEventType type) {

        OutboxEventHandler<?> handler = handlers.get(type);

        if (handler == null) {
            throw new IllegalStateException("No handler registered for " + type);
        }

        return handler;
    }
}
