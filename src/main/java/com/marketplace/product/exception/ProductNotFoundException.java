package com.marketplace.product.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("Product with id %d does not exist".formatted(id));
    }}
