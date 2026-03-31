CREATE SCHEMA IF NOT EXISTS orders;

CREATE TABLE IF NOT EXISTS orders.orders (
                                             order_id           UUID PRIMARY KEY,
                                             username           VARCHAR(255) NOT NULL,
    shopping_cart_id   UUID,
    payment_id         UUID,
    delivery_id        UUID,

    state              VARCHAR(50) NOT NULL,

    delivery_weight    DOUBLE PRECISION,
    delivery_volume    DOUBLE PRECISION,
    fragile            BOOLEAN NOT NULL DEFAULT FALSE,

    total_price        NUMERIC(19, 2),
    delivery_price     NUMERIC(19, 2),
    product_price      NUMERIC(19, 2)
    );

CREATE TABLE IF NOT EXISTS orders.order_items (
                                                  order_id   UUID NOT NULL
                                                  REFERENCES orders.orders (order_id),
    product_id UUID NOT NULL,
    quantity   BIGINT NOT NULL,

    PRIMARY KEY (order_id, product_id)
    );