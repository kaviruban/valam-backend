-- Replace the hand-typed SKU with identifiers the database generates:
--   products.product_code   human-friendly product ID, e.g. PRD-000001
--   product_variants.isbn   13-digit barcode number per pack size (EAN-13 format, valid check digit)

CREATE SEQUENCE product_code_seq;

-- The default is evaluated per row, so existing products are numbered too.
ALTER TABLE products
    ADD COLUMN product_code VARCHAR(20) NOT NULL
        DEFAULT 'PRD-' || LPAD(nextval('product_code_seq')::TEXT, 6, '0');
ALTER TABLE products ADD CONSTRAINT uq_products_product_code UNIQUE (product_code);

CREATE SEQUENCE variant_isbn_seq;

-- Prefix 200 is the GS1 "in-store" range, so these numbers never clash with codes printed on
-- branded goods (or real book ISBNs, which start with 978/979). 3 prefix + 9 sequence + 1 check digit.
CREATE FUNCTION next_variant_isbn() RETURNS VARCHAR(13)
    LANGUAGE plpgsql AS
$$
DECLARE
    body  TEXT    := '200' || LPAD(nextval('variant_isbn_seq')::TEXT, 9, '0');
    total INTEGER := 0;
BEGIN
    FOR i IN 1..12 LOOP
        total := total + SUBSTR(body, i, 1)::INTEGER * CASE WHEN i % 2 = 0 THEN 3 ELSE 1 END;
    END LOOP;
    RETURN body || ((10 - total % 10) % 10)::TEXT;
END;
$$;

ALTER TABLE product_variants
    ADD COLUMN isbn VARCHAR(13) NOT NULL DEFAULT next_variant_isbn();
ALTER TABLE product_variants ADD CONSTRAINT uq_product_variants_isbn UNIQUE (isbn);

DROP INDEX uq_product_variants_sku_ci;
ALTER TABLE product_variants DROP COLUMN sku;
