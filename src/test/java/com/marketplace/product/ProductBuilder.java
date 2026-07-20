package com.marketplace.product;

import java.math.BigDecimal;

public class ProductBuilder {
    private Long id = 1L;
    private Long version = 0L;
    private String name = "chair";
    private BigDecimal price = new BigDecimal(40);
    private int inventory = 10;

    public static ProductBuilder aProduct() {
        return new ProductBuilder();
    }

    public ProductBuilder withId(Long id) {
       this.id = id;
       return this;
    }

    public ProductBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ProductBuilder withPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public ProductBuilder withInventory(int inventory) {
        this.inventory = inventory;
        return this;
    }

    public ProductBuilder withVersion(Long version) {
        this.version = version;
        return this;
    }

    public Product build() {
        Product product = new Product();
        product.setId(id);
        product.setVersion(version);
        product.setName(name);
        product.setPrice(price);
        product.setInventory(inventory);

        return product;
    }
}
