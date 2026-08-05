package com.marketplace.product.repository;

import com.marketplace.annotations.RepositoryTest;
import com.marketplace.product.Product;
import com.marketplace.product.ProductBuilder;
import com.marketplace.product.ProductSnapshot;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

@RepositoryTest
class ProductRepositoryTest {

    @Autowired
    ProductRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Nested
    class ReserveInventoryTest {

        @Test
        void shouldReserveInventory() {
            Product product = new ProductBuilder()
                    .withId(null)
                    .withVersion(1L)
                    .withInventory(10)
                    .build();

            Long id = (Long) entityManager.persistAndGetId(product);

            Optional<ProductSnapshot> optionalProductSnapshot = repository.reserveInventory(
                    id,
                    product.getVersion(),
                    4
            );

            assertThat(optionalProductSnapshot).isPresent();
            ProductSnapshot productSnapshot = optionalProductSnapshot.get();

            assertThat(productSnapshot.name()).isEqualTo(product.getName());
            assertThat(productSnapshot.price()).usingComparator(
                    BigDecimal::compareTo
            ).isEqualTo(product.getPrice());

            entityManager.clear();

            Optional<Product> optionalNewProduct = repository.findById(id);
            assertThat(optionalNewProduct).isPresent();

            Product newProduct = optionalNewProduct.get();
            assertThat(newProduct.getVersion()).isEqualTo(product.getVersion() + 1);
            assertThat(newProduct.getInventory()).isEqualTo(6);
        }

        void assertThatProductHasNotChanged(Long id, Product product) {
            Optional<Product> optionalNewProduct = repository.findById(id);
            assertThat(optionalNewProduct).isNotEmpty();
            Product newProduct = optionalNewProduct.get();
            assertThat(newProduct).isEqualTo(product);
        }

        @Test
        void shouldNotReserveInventoryWhenProdutDoesNotExist() {
            Product product = new ProductBuilder()
                    .withId(null)
                    .build();

            Long id = (Long) entityManager.persistAndGetId(product);

            Optional<ProductSnapshot> optionalProductSnapshot = repository.reserveInventory(
                    id + 1,
                    product.getVersion(),
                    4
            );

            assertThat(optionalProductSnapshot).isEmpty();
            assertThatProductHasNotChanged(id, product);
        }


        @Test
        void shouldNotReserveInventoryWhenVersionsMismatch() {
            Product product = new ProductBuilder()
                    .withId(null)
                    .withVersion(10L)
                    .build();

            Long id = (Long) entityManager.persistAndGetId(product);

            Optional<ProductSnapshot> optionalProductSnapshot = repository.reserveInventory(
                    id,
                    product.getVersion() - 1,
                    4
            );
            assertThat(optionalProductSnapshot).isEmpty();
            assertThatProductHasNotChanged(id, product);
        }

        @Test
        void shouldNotReserveInventoryWhenInsufficient() {
            Product product = new ProductBuilder()
                    .withId(null)
                    .withVersion(10L)
                    .withInventory(10)
                    .build();

            Long id = (Long) entityManager.persistAndGetId(product);

            Optional<ProductSnapshot> optionalProductSnapshot = repository.reserveInventory(
                    id,
                    product.getVersion(),
                    11
            );
            assertThat(optionalProductSnapshot).isEmpty();
            assertThatProductHasNotChanged(id, product);
        }

        @Test
        void shouldReserveInventoryWhenExactlyEqualToReservation() {
            Product product = new ProductBuilder()
                    .withId(null)
                    .withVersion(10L)
                    .withInventory(10)
                    .build();

            Long id = (Long) entityManager.persistAndGetId(product);

            repository.reserveInventory(
                    id,
                    product.getVersion(),
                    10
            );

            entityManager.flush();
            entityManager.clear();

            Optional<Product> optionalNewProduct = repository.findById(id);
            assertThat(optionalNewProduct).isNotEmpty();
        }
    }
}