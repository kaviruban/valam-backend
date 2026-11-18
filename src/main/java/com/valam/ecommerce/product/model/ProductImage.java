package com.valam.ecommerce.product.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One picture in a product's gallery. The first image in the list is the main one. */
@Embeddable
public class ProductImage {

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "alt_text")
    private String altText;

    protected ProductImage() {
    }

    public ProductImage(String url, String altText) {
        this.url = url;
        this.altText = altText;
    }

    public String getUrl() {
        return url;
    }

    public String getAltText() {
        return altText;
    }
}
