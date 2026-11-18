package com.valam.ecommerce.product.model;

/** What a product is measured by. Each type has a base unit that quantities are stored in. */
public enum MeasureType {
    WEIGHT(QuantityUnit.G),
    VOLUME(QuantityUnit.ML);

    private final QuantityUnit baseUnit;

    MeasureType(QuantityUnit baseUnit) {
        this.baseUnit = baseUnit;
    }

    public QuantityUnit getBaseUnit() {
        return baseUnit;
    }
}
