package com.marketplace.product;

import com.marketplace.product.dto.CreateProductRequest;
import com.marketplace.product.dto.CreateProductResponse;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    CreateProductRequest toCreateProductRequest(Product product);
    CreateProductResponse toCreateProductResponse(Product product);
    Product toProduct(CreateProductRequest createProductRequest);

    UpdateProductRequest toUpdateProductRequest(Product product);
    UpdateProductResponse toUpdateProductResponse(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "version", ignore = true)
    Product updateProduct(UpdateProductRequest updateUserRequest, @MappingTarget Product product);
}


