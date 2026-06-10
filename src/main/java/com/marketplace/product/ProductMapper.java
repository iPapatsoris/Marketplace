package com.marketplace.product;

import com.marketplace.product.dto.CreateProductRequest;
import com.marketplace.product.dto.CreateProductResponse;
import com.marketplace.product.dto.UpdateProductRequest;
import com.marketplace.product.dto.UpdateProductResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    CreateProductResponse toCreateProductResponse(Product entity);
    Product toEntity(CreateProductRequest dto);

    UpdateProductResponse toUpdateProductResponse(Product entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "version", ignore = true)
    Product updateProduct(UpdateProductRequest dto, @MappingTarget Product entity);
}


