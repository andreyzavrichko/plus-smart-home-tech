CREATE SCHEMA IF NOT EXISTS delivery;

CREATE TABLE IF NOT EXISTS delivery.deliveries (
                                                   delivery_id     UUID PRIMARY KEY,
                                                   order_id        UUID NOT NULL,
                                                   delivery_state  VARCHAR(50) NOT NULL,

    from_country    VARCHAR(255),
    from_city       VARCHAR(255),
    from_street     VARCHAR(255),
    from_house      VARCHAR(50),
    from_flat       VARCHAR(50),

    to_country      VARCHAR(255),
    to_city         VARCHAR(255),
    to_street       VARCHAR(255),
    to_house        VARCHAR(50),
    to_flat         VARCHAR(50)
    );