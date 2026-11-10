CREATE TABLE house_wallet (
    id INT NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_house_wallet_id CHECK (id = 1),
    CONSTRAINT chk_house_wallet_balance CHECK (balance >= 0)
);

INSERT INTO house_wallet (id, balance) VALUES (1, 0.00);
