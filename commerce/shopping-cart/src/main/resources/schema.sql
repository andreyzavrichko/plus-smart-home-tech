CREATE SCHEMA IF NOT EXISTS shopping_cart;

CREATE TABLE IF NOT EXISTS shopping_cart.shopping_carts (
    shopping_cart_id UUID         PRIMARY KEY,
    username         VARCHAR(255) NOT NULL,
    active           BOOLEAN      NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS shopping_cart.shopping_cart_items (
    shopping_cart_id UUID   NOT NULL REFERENCES shopping_cart.shopping_carts(shopping_cart_id),
    product_id       UUID   NOT NULL,
    quantity         BIGINT NOT NULL,

    PRIMARY KEY (shopping_cart_id, product_id)
    );