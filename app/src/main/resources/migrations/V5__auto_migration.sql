CREATE TABLE IF NOT EXISTS categories
(
    id
    VARCHAR
(
    10
) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, "name" VARCHAR
(
    255
) NOT NULL, slug VARCHAR
(
    255
) NOT NULL, description VARCHAR
(
    255
) NULL, image_url VARCHAR
(
    255
) NULL, is_active BOOLEAN NOT NULL, display_order INT NOT NULL);

ALTER TABLE categories
    ADD CONSTRAINT categories_id_unique UNIQUE (id);

ALTER TABLE categories
    ADD CONSTRAINT categories_name_unique UNIQUE ("name");

ALTER TABLE categories
    ADD CONSTRAINT categories_slug_unique UNIQUE (slug);

CREATE TABLE IF NOT EXISTS products
(
    id
    VARCHAR
(
    10
) NOT NULL, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL, sku VARCHAR
(
    100
) NOT NULL, "name" VARCHAR
(
    255
) NOT NULL, description VARCHAR
(
    255
) NOT NULL, short_description VARCHAR
(
    255
) NOT NULL, base_price DECIMAL
(
    10,
    2
) NOT NULL, viewed BOOLEAN DEFAULT FALSE NOT NULL, category_id VARCHAR
(
    10
) NOT NULL, stock_available INT NOT NULL, stock_low_threshold INT DEFAULT 3 NOT NULL, meta_title VARCHAR
(
    255
) NOT NULL, meta_description VARCHAR
(
    255
) NOT NULL, seo_slug VARCHAR
(
    255
) NOT NULL, canonical_url VARCHAR
(
    255
) NULL, keywords TEXT[] NOT NULL, CONSTRAINT fk_products_category_id__id FOREIGN KEY
(
    category_id
) REFERENCES categories
(
    id
) ON DELETE RESTRICT
  ON UPDATE RESTRICT);

ALTER TABLE products
    ADD CONSTRAINT products_id_unique UNIQUE (id);

ALTER TABLE products
    ADD CONSTRAINT products_sku_unique UNIQUE (sku);