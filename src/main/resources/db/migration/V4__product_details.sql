-- Richer product details: storefront URL slug, image gallery, highlights, product information,
-- tax fields, SEO, featured flag, optimistic locking; SKU and MRP per pack size.

ALTER TABLE products
    ADD COLUMN slug                 VARCHAR(200),
    ADD COLUMN short_description    VARCHAR(500),
    ADD COLUMN ingredients          TEXT,
    ADD COLUMN usage_instructions   TEXT,
    ADD COLUMN storage_instructions TEXT,
    ADD COLUMN shelf_life           VARCHAR(100),
    ADD COLUMN country_of_origin    VARCHAR(100) NOT NULL DEFAULT 'India',
    ADD COLUMN hsn_code             VARCHAR(8),
    ADD COLUMN gst_rate             NUMERIC(5, 2) CHECK (gst_rate >= 0 AND gst_rate <= 100),
    ADD COLUMN meta_title           VARCHAR(100),
    ADD COLUMN meta_description     VARCHAR(300),
    ADD COLUMN featured             BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN version              BIGINT       NOT NULL DEFAULT 0;

-- Existing rows get a placeholder slug the admin can change later.
UPDATE products SET slug = 'product-' || id WHERE slug IS NULL;
ALTER TABLE products ALTER COLUMN slug SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT uq_products_slug UNIQUE (slug);

CREATE INDEX idx_products_featured ON products (featured) WHERE featured;

-- Image gallery; position 0 is the main image.
CREATE TABLE product_images
(
    product_id BIGINT        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    position   INTEGER       NOT NULL,
    url        VARCHAR(1000) NOT NULL,
    alt_text   VARCHAR(255),
    PRIMARY KEY (product_id, position)
);

INSERT INTO product_images (product_id, position, url)
SELECT id, 0, image_url FROM products WHERE image_url IS NOT NULL;

ALTER TABLE products DROP COLUMN image_url;

-- Short bullet points shown near the price ("100% natural", "No preservatives").
CREATE TABLE product_highlights
(
    product_id BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    position   INTEGER      NOT NULL,
    text       VARCHAR(255) NOT NULL,
    PRIMARY KEY (product_id, position)
);

-- SKU identifies a pack size for stock keeping; MRP is the printed maximum retail price.
ALTER TABLE product_variants
    ADD COLUMN sku VARCHAR(64),
    ADD COLUMN mrp NUMERIC(12, 2),
    ADD CONSTRAINT chk_variant_mrp CHECK (mrp IS NULL OR mrp >= price);

CREATE UNIQUE INDEX uq_product_variants_sku_ci ON product_variants (UPPER(sku)) WHERE sku IS NOT NULL;
