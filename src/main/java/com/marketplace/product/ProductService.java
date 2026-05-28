package com.marketplace.product;

import com.marketplace.product.dto.CreateProductRequest;
import com.marketplace.product.dto.CreateProductResponse;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import com.marketplace.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    public CreateProductResponse createProduct(CreateProductRequest createProductRequest) {
        Product product = new Product(createProductRequest.name(), createProductRequest.price(), createProductRequest.inventory());
        productRepository.save(product);
        return productMapper.toCreateProductResponse(product);
    }

    public UpdateProductResponse updateProduct(Long id, UpdateProductRequest updateProductRequest) {
       var product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException("Product with id %s does not exist".formatted(id)));
       var updatedProduct = productMapper.updateProduct(updateProductRequest, product);
       productRepository.save(updatedProduct);

       return productMapper.toUpdateProductResponse(updatedProduct);
    }
}
