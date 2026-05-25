package com.marketplace.product;

import com.marketplace.product.dto.CreateProductRequest;
import com.marketplace.product.dto.CreateProductResponse;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    CreateProductRequest toCreateProductRequest(Product product);
    CreateProductResponse toCreateProductResponse(Product product);

    UpdateProductRequest toUpdateProductRequest(Product product);
    UpdateProductResponse toUpdateProductResponse(Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Product updateProduct(UpdateProductRequest updateUserRequest, @MappingTarget Product product);
}


