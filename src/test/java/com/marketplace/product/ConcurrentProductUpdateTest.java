package com.marketplace.product;

import com.marketplace.annotations.SpringIntegrationTest;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import com.marketplace.product.repository.ProductRepository;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static com.marketplace.util.ConcurrencyControl.await;
import static org.assertj.core.api.Assertions.assertThat;

@SpringIntegrationTest
public class ConcurrentProductUpdateTest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Product product;

    @BeforeEach
    void setup() {
        product = new ProductBuilder()
                .withVersion(10L)
                .withName("chair")
                .withPrice(new BigDecimal("30.00"))
                .withInventory(5)
                .build();

        product = productRepository.save(product);
    }

    @AfterEach
    void cleanup() {
        productRepository.deleteAll();
    }

    @Test
    void shouldLetOneUpdateThrough() throws InterruptedException {
        int requestCount = 10;
        UpdateProductRequest[] requests = new UpdateProductRequest[requestCount];
        EntityExchangeResult<byte[]>[] responses = new EntityExchangeResult[requestCount];
        Future<?>[] threadResults = new Future[requestCount];

        for (int i = 0; i < requestCount; i++) {
            requests[i] =
                    new UpdateProductRequest(
                            product.getVersion(),
                            "new name " + i,
                            null,
                            null
                    );
        }

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        CyclicBarrier barrier = new CyclicBarrier(requestCount);

        Consumer<Integer> worker = index -> {
            await(barrier);
            UpdateProductRequest request = requests[index];
            responses[index] = restClient.patch().uri("/products/{id}", product.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange()
                    .expectBody()
                    .returnResult();
        };

        try {
            for (int i = 0; i < requestCount; i++) {
                final int requestIndex = i;
                Runnable thread = () -> {
                    worker.accept(requestIndex);
                };
                threadResults[i] = executor.submit(thread);
            }

            for (Future<?> result : threadResults) {
                result.get();
            }
        }
        catch (ExecutionException e) {
            throw new AssertionFailure("Worker thread failed", e.getCause());
        }
        // No shutdown/shutdownNow needed.
        // If a worker fails, Future.get() returns immediately with an ExecutionException,
        // while the remaining workers complete naturally. This test has no long-running
        // or blocking work that requires interruption.


        Predicate<EntityExchangeResult<byte[]>> isFailedUpdate =
                response -> {
                    if (response.getStatus() != HttpStatus.CONFLICT) {
                        return false;
                    }
                    ProblemDetail responseDto = objectMapper.readValue(
                            response.getResponseBody(), ProblemDetail.class);
                    return responseDto.getTitle().equals("Product version conflict");
                };

        var successIndexes = IntStream.range(0, requestCount)
                        .filter(i -> responses[i].getStatus() == HttpStatus.OK)
                        .toArray();

        // Assert number of successes and failures
        assertThat(successIndexes).hasSize(1);
        assertThat(responses).filteredOn(isFailedUpdate).hasSize(requestCount - 1);

        int successIndex = successIndexes[0];
        UpdateProductResponse successResponse = objectMapper.readValue(
                responses[successIndex].getResponseBody(), UpdateProductResponse.class);

        // Assert success fields
        assertThat(successResponse.version()).isEqualTo(product.getVersion()+1);
        assertThat(successResponse.name()).isEqualTo(requests[successIndex].name());
        assertThat(successResponse.price()).isEqualTo(product.getPrice());
        assertThat(successResponse.inventory()).isEqualTo(product.getInventory());

        Product updated = productRepository.findById(product.getId()).orElseThrow();

        // Assert persisted state
        assertThat(updated.getVersion())
                .isEqualTo(product.getVersion() + 1);
        assertThat(updated.getName())
                .isEqualTo(successResponse.name());
    }
}
