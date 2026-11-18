-- Products now carry a category; price/stock move to per-pack-size variants.

ALTER TABLE products
    ADD COLUMN category  VARCHAR(20) NOT NULL DEFAULT 'FOOD',
    ADD COLUMN image_url VARCHAR(1000),
    ADD COLUMN active    BOOLEAN     NOT NULL DEFAULT TRUE;

ALTER TABLE products ALTER COLUMN category DROP DEFAULT;

ALTER TABLE products
    ADD CONSTRAINT chk_products_category
        CHECK (category IN ('FOOD', 'POWDER', 'RAW', 'LIQUID'));

ALTER TABLE products
    DROP COLUMN price,
    DROP COLUMN stock_quantity;

CREATE INDEX idx_products_category_active ON products (category, active);

-- One row per sellable pack size, e.g. 500 g, 1 kg, 250 ml.
-- quantity is stored in the category's base unit: grams (FOOD/POWDER/RAW) or millilitres (LIQUID).
CREATE TABLE product_variants
(
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT         NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity       INTEGER        NOT NULL CHECK (quantity > 0),
    price          NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER        NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Deferred so an update can swap/replace pack sizes within one transaction.
    CONSTRAINT uq_variant_product_quantity UNIQUE (product_id, quantity) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_product_variants_product_id ON product_variants (product_id);
