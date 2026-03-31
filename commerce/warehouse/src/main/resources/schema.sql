CREATE SCHEMA IF NOT EXISTS warehouse;

CREATE TABLE IF NOT EXISTS warehouse.warehouse_products (
                                                            product_id  UUID PRIMARY KEY,
                                                            fragile     BOOLEAN NOT NULL DEFAULT FALSE,

                                                            weight      DOUBLE PRECISION NOT NULL,
                                                            width       DOUBLE PRECISION NOT NULL,
                                                            height      DOUBLE PRECISION NOT NULL,
                                                            depth       DOUBLE PRECISION NOT NULL,

                                                            quantity    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS warehouse.order_bookings (
                                                        booking_id  UUID PRIMARY KEY,
                                                        order_id    UUID NOT NULL UNIQUE,
                                                        delivery_id UUID
);

CREATE TABLE IF NOT EXISTS warehouse.order_booking_items (
                                                             booking_id  UUID NOT NULL
                                                             REFERENCES warehouse.order_bookings (booking_id),
    product_id  UUID NOT NULL,
    quantity    BIGINT NOT NULL,

    PRIMARY KEY (booking_id, product_id)
    );