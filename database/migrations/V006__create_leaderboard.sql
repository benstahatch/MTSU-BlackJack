CREATE TABLE leaderboard (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(50) NOT NULL,
    peak_balance DECIMAL(15,2) NOT NULL DEFAULT 1000.00,
    wins BIGINT NOT NULL DEFAULT 0,
    peak_achieved_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_leaderboard_user UNIQUE (user_id),
    CONSTRAINT fk_leaderboard_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_leaderboard_peak CHECK (peak_balance >= 0),
    CONSTRAINT chk_leaderboard_wins CHECK (wins >= 0),
    INDEX idx_leaderboard_rank (peak_balance DESC, peak_achieved_at ASC, id ASC)
);

-- Older peak balances cannot be reconstructed reliably. Seed from the saved wallet.
INSERT INTO leaderboard (user_id, username, peak_balance, wins)
SELECT u.id, u.username, GREATEST(COALESCE(w.balance, 1000.00), 1000.00),
       (SELECT COUNT(*) FROM game_history h WHERE h.user_id = u.id AND h.result = 'PLAYER_WIN')
FROM users u LEFT JOIN wallets w ON w.user_id = u.id;
