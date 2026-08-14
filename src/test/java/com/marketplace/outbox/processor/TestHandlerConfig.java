package com.marketplace.outbox.processor;

import com.marketplace.outbox.OutboxEventHandler;
import com.marketplace.outbox.OutboxEventType;
import com.marketplace.outbox.exception.OutboxEventFailureException;
import com.marketplace.product.ProductBuilder;
import com.marketplace.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.stereotype.Component;

import static com.marketplace.outbox.processor.OutboxEventProcessorTest.productNameThatShouldExist;
import static com.marketplace.outbox.processor.OutboxEventProcessorTest.productNameThatShouldNotExist;

@TestConfiguration
public class TestHandlerConfig {
    public record DummyPayload(Long dummy) { }

    @Component
    static class DummyHandler1 implements OutboxEventHandler<DummyPayload> {

        @Autowired
        private ProductRepository dummyRepository;

        @Override
        public OutboxEventType supports() {
            return OutboxEventType.DUMMY1;
        }

        @Override
        public Class<DummyPayload> payloadClass() {
            return DummyPayload.class;
        }

        @Override
        public void handle(DummyPayload payload) {
            dummyRepository.save(
                    new ProductBuilder().withName(productNameThatShouldExist).build()
            );
        }
    }

    @Component
    static class DummyHandler2 implements OutboxEventHandler<DummyPayload> {

        @Autowired
        private ProductRepository dummyRepository;

        @Override
        public OutboxEventType supports() {
            return OutboxEventType.DUMMY2;
        }

        @Override
        public Class<DummyPayload> payloadClass() {
            return DummyPayload.class;
        }

        @Override
        public void handle(DummyPayload payload) {
            dummyRepository.save(
                    new ProductBuilder().withName(productNameThatShouldNotExist).build()
            );
            throw new RuntimeException("Dummy exception that should rollback");
        }
    }

    @Component
    static class DummyHandler3 implements OutboxEventHandler<DummyPayload> {

        @Autowired
        private ProductRepository dummyRepository;

        @Override
        public OutboxEventType supports() {
            return OutboxEventType.DUMMY3;
        }

        @Override
        public Class<DummyPayload> payloadClass() {
            return DummyPayload.class;
        }

        @Override
        public void handle(DummyPayload payload) {
            dummyRepository.save(
                    new ProductBuilder().withName(productNameThatShouldExist).build()
            );
            throw new OutboxEventFailureException("Dummy exception that should NOT rollback");
        }
    }
}

