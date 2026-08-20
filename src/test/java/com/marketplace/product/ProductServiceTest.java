package com.marketplace.product;

import com.marketplace.product.dto.CreateProductRequest;
import com.marketplace.product.dto.CreateProductResponse;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import com.marketplace.product.exception.ProductNotFoundException;
import com.marketplace.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;

    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);
    private ProductService productService;

    @BeforeEach
    void setup() {
        productService = new ProductService(productMapper, productRepository);
    }

    @Test
    void shouldCreateProduct() {
        String name = "chair";
        BigDecimal price = new BigDecimal(30);
        int inventory = 10;
        CreateProductRequest request = new CreateProductRequest(
                name, price, inventory
        );

        Long newProductId = 2L;
        CreateProductResponse expectedResponse = new CreateProductResponse(
                newProductId, name, price, inventory
        );

        when(productRepository.save(any(Product.class))).thenAnswer(
                invocationOnMock -> {
                    Product product = invocationOnMock.getArgument(0);
                    product.setId(newProductId);
                    return product;
                }
        );

        CreateProductResponse response = productService.createProduct(request);
        assertEquals(expectedResponse, response);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldUpdateProduct() {
        Long id = 2L;
        Product initialProduct = new ProductBuilder()
                .withId(id)
                .withName("chair")
                .withPrice(new BigDecimal(30))
                .withInventory(10)
                .withVersion(20L)
                .build();

        String requestName = "comfy chair";
        BigDecimal requestPrice = null; // Important to ensure null request fields don't update their entity corresponding ones
        int requestInventory = 12;

        UpdateProductRequest request = new UpdateProductRequest(
               initialProduct.version, requestName, requestPrice, requestInventory
        );

        // Version is just a placeholder, it is ignored
        UpdateProductResponse expectedResponseExcludingVersion = new UpdateProductResponse(
                0L, requestName, initialProduct.price, requestInventory
        );

        when(productRepository.findById(id)).thenReturn(Optional.of(initialProduct));

        UpdateProductResponse response = productService.updateProduct(id, request);

        assertThat(response)
                .usingRecursiveComparison()
                .ignoringFields("version")
                .isEqualTo(expectedResponseExcludingVersion);
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenProductDoesNotExist() {
        Long id = 0L;
        UpdateProductRequest reservationRequest = new UpdateProductRequest(
            0L, "", null, null
        );
        when(productRepository.findById(id)).thenReturn(Optional.empty());
        assertThrowsExactly(ProductNotFoundException.class, () ->
                productService.updateProduct(id, reservationRequest));
    }

    @Test
    void shouldThrowOptimisticLockExceptionWhenProductHasChanged() {
        Long id = 0L;
        Long requestVersion = 1L;
        Long actualVersion = 2L;

        Product product = new ProductBuilder()
                .withVersion(actualVersion).build();

        UpdateProductRequest request = new UpdateProductRequest(
                requestVersion, "", null, null
        );

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        assertThrowsExactly(OptimisticLockException.class, () ->
                productService.updateProduct(id, request));
    }
}

