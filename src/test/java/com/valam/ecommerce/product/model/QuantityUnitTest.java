package com.valam.ecommerce.product.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuantityUnitTest {

    @Test
    void convertsToBaseUnit() {
        assertEquals(500, QuantityUnit.G.toBase(new BigDecimal("500")));
        assertEquals(1000, QuantityUnit.KG.toBase(new BigDecimal("1")));
        assertEquals(1500, QuantityUnit.KG.toBase(new BigDecimal("1.5")));
        assertEquals(250, QuantityUnit.ML.toBase(new BigDecimal("250")));
        assertEquals(2000, QuantityUnit.L.toBase(new BigDecimal("2")));
    }

    @Test
    void rejectsFractionalBaseQuantity() {
        assertThrows(ArithmeticException.class, () -> QuantityUnit.G.toBase(new BigDecimal("0.5")));
        assertThrows(ArithmeticException.class, () -> QuantityUnit.KG.toBase(new BigDecimal("0.0005")));
    }

    @Test
    void buildsLabels() {
        assertEquals("500 g", QuantityUnit.label(500, MeasureType.WEIGHT));
        assertEquals("1 kg", QuantityUnit.label(1000, MeasureType.WEIGHT));
        assertEquals("1.5 kg", QuantityUnit.label(1500, MeasureType.WEIGHT));
        assertEquals("200 ml", QuantityUnit.label(200, MeasureType.VOLUME));
        assertEquals("1 L", QuantityUnit.label(1000, MeasureType.VOLUME));
    }
}
