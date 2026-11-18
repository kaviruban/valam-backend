package com.valam.ecommerce.product.model;

import java.math.BigDecimal;

/** Units a seller may enter a pack size in. Converted to the base unit (g / ml) for storage. */
public enum QuantityUnit {
    G("g", 1),
    KG("kg", 1000),
    ML("ml", 1),
    L("L", 1000);

    private final String symbol;
    private final int toBaseFactor;

    QuantityUnit(String symbol, int toBaseFactor) {
        this.symbol = symbol;
        this.toBaseFactor = toBaseFactor;
    }

    public String getSymbol() {
        return symbol;
    }

    public MeasureType getMeasureType() {
        return (this == G || this == KG) ? MeasureType.WEIGHT : MeasureType.VOLUME;
    }

    /**
     * Converts a quantity in this unit to the base unit (g or ml).
     *
     * @throws ArithmeticException if the result is not a whole number of base units
     */
    public int toBase(BigDecimal quantity) {
        return quantity.multiply(BigDecimal.valueOf(toBaseFactor)).intValueExact();
    }

    /** Human-readable label for a base-unit quantity, e.g. 500 -> "500 g", 1500 -> "1.5 kg". */
    public static String label(int baseQuantity, MeasureType type) {
        QuantityUnit small = type.getBaseUnit();
        QuantityUnit large = (type == MeasureType.WEIGHT) ? KG : L;
        if (baseQuantity >= large.toBaseFactor) {
            BigDecimal value = BigDecimal.valueOf(baseQuantity)
                    .divide(BigDecimal.valueOf(large.toBaseFactor))
                    .stripTrailingZeros();
            return value.toPlainString() + " " + large.symbol;
        }
        return baseQuantity + " " + small.symbol;
    }
}
