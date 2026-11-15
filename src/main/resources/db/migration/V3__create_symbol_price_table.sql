CREATE TABLE symbol_price (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    symbol_id BIGINT NOT NULL,
    price_date DATE NOT NULL,
    open NUMERIC(20, 8) NOT NULL,
    high NUMERIC(20, 8) NOT NULL,
    low NUMERIC(20, 8) NOT NULL,
    close NUMERIC(20, 8) NOT NULL,
    adjusted_close NUMERIC(20, 8) NOT NULL,
    volume BIGINT NOT NULL,
    CONSTRAINT fk_symbol_price_symbol
        FOREIGN KEY (symbol_id) REFERENCES symbol (id)
        ON DELETE CASCADE,
    CONSTRAINT uk_symbol_price_symbol_date
        UNIQUE (symbol_id, price_date)
);
