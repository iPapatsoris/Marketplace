package com.marketplace.product;

import com.marketplace.product.dto.CreateProductRequest;
import com.marketplace.product.dto.CreateProductResponse;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ProductService service;

    @Nested
    class CreateProductTest {
        @Test
        void shouldCreateProduct() throws Exception {
            Product product = new ProductBuilder()
                    .withId(1L)
                    .withName("chair")
                    .withPrice(new BigDecimal(30L))
                    .withInventory(10)
                    .build();

            CreateProductRequest request = new CreateProductRequest(
                    product.getName(), product.getPrice(), product.inventory
            );
            CreateProductResponse mockedResponse = new CreateProductResponse(
                    product.getId(), product.getName(), product.getPrice(), product.inventory
            );

            when(service.createProduct(request))
                    .thenReturn(mockedResponse);

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(product.getId()));
        }

        @Test
        void shouldRejectInvalidCreateRequest() throws Exception {
            CreateProductRequest request =
                    new CreateProductRequest(
                            "",
                            null,
                            -1
                    );

            mockMvc.perform(post("/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }
    }

    @Nested
    class UpdateProductTest {
        @Test
        void shouldUpdateProduct() throws Exception {
            Product requestProduct = new ProductBuilder()
                    .withId(1L)
                    .withVersion(5L)
                    .withName("chair")
                    .withPrice(new BigDecimal(30L))
                    .withInventory(10)
                    .build();

            UpdateProductRequest request = new UpdateProductRequest(
                    requestProduct.getVersion(),
                    requestProduct.getName(),
                    requestProduct.getPrice(),
                    requestProduct.getInventory()
            );
            UpdateProductResponse mockedResponse = new UpdateProductResponse(
                    requestProduct.getVersion()+1, requestProduct.getName(), requestProduct.getPrice(), requestProduct.getInventory());

            when(service.updateProduct(requestProduct.getId(), request))
                    .thenReturn(mockedResponse);

            mockMvc.perform(patch("/products/" + requestProduct.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        void shouldRejectInvalidUpdateRequest() throws Exception {
            UpdateProductRequest request =
                    new UpdateProductRequest(
                            null,
                            null,
                            new BigDecimal(-1),
                            -1
                    );

            mockMvc.perform(patch("/products/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(service);
        }
    }
}
