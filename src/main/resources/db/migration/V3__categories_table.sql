-- Categories move from a hard-coded enum to a table so the admin can add new ones.

CREATE TABLE categories
(
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    -- Decides which pack-size units products in this category use: WEIGHT (g/kg) or VOLUME (ml/L).
    measure_type VARCHAR(20)  NOT NULL CHECK (measure_type IN ('WEIGHT', 'VOLUME')),
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    -- Order in the dropdown; lower comes first.
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uq_categories_name_ci ON categories (LOWER(name));

INSERT INTO categories (name, measure_type, sort_order)
VALUES ('Food Products', 'WEIGHT', 10),
       ('Snacks', 'WEIGHT', 20),
       ('Powder Products', 'WEIGHT', 30),
       ('Cow Dung Products', 'WEIGHT', 40),
       ('Traditional Seeds', 'WEIGHT', 50),
       ('Everyday Essential Products', 'WEIGHT', 60);

-- Point products at the new table.
ALTER TABLE products
    ADD COLUMN category_id BIGINT REFERENCES categories (id) ON DELETE RESTRICT;

-- Old codes with an equivalent new category. RAW / LIQUID have none; any such rows
-- make the SET NOT NULL below fail, so they must be re-categorised by hand first.
UPDATE products p
SET category_id = c.id
FROM categories c
WHERE (p.category = 'FOOD' AND c.name = 'Food Products')
   OR (p.category = 'POWDER' AND c.name = 'Powder Products');

ALTER TABLE products ALTER COLUMN category_id SET NOT NULL;

DROP INDEX idx_products_category_active;
ALTER TABLE products DROP CONSTRAINT chk_products_category;
ALTER TABLE products DROP COLUMN category;

CREATE INDEX idx_products_category_id_active ON products (category_id, active);
