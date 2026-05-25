package com.marketplace.product;

import com.marketplace.product.dto.CreateProductRequest;
import com.marketplace.product.dto.CreateProductResponse;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public CreateProductResponse createProduct(@RequestBody CreateProductRequest createProductRequest) {
        return productService.createProduct(createProductRequest);
    }

    @PatchMapping("/{id}")
    public UpdateProductResponse updateProduct(@PathVariable Long productID, @RequestBody UpdateProductRequest updateProductRequest) {
        return productService.updateProduct(productID, updateProductRequest);
    }
}
