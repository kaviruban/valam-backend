package com.valam.ecommerce.product.mapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductMapperTest {

    @Test
    void discountIsRoundedDown() {
        assertEquals(15, ProductMapper.discountPercent(new BigDecimal("85"), new BigDecimal("100")));
        assertEquals(33, ProductMapper.discountPercent(new BigDecimal("199"), new BigDecimal("299")));
    }

    @Test
    void noDiscountWithoutMrpOrSaving() {
        assertNull(ProductMapper.discountPercent(new BigDecimal("100"), null));
        assertNull(ProductMapper.discountPercent(new BigDecimal("100"), new BigDecimal("100")));
        assertNull(ProductMapper.discountPercent(new BigDecimal("99.5"), new BigDecimal("100")));
    }
}
