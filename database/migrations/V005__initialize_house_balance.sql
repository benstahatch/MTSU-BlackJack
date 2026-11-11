INSERT INTO house_wallet (id, balance) VALUES (1, 100000.00)
ON DUPLICATE KEY UPDATE balance = 100000.00;
