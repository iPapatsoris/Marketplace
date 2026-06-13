package com.marketplace.product;

import com.marketplace.product.dto.CreateProductRequest;
import com.marketplace.product.dto.CreateProductResponse;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import com.marketplace.product.exception.ProductNotFoundException;
import com.marketplace.product.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    public CreateProductResponse createProduct(CreateProductRequest createProductRequest) {
        Product product = productMapper.toEntity(createProductRequest);
        productRepository.save(product);

        return productMapper.toCreateProductResponse(product);
    }

    @Transactional
    public UpdateProductResponse updateProduct(Long id, UpdateProductRequest updateProductRequest) {
       var product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        if (!Objects.equals(product.version, updateProductRequest.version())) {
            throw new OptimisticLockException("Product with id %d has changed".formatted(id));
        }
       var updatedProduct = productMapper.updateProduct(updateProductRequest, product);
       productRepository.save(updatedProduct);

       return productMapper.toUpdateProductResponse(updatedProduct);
    }
}
