CREATE TABLE game_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    round_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    hand_index INT NOT NULL,
    result VARCHAR(20) NOT NULL,
    bet_amount DECIMAL(15,2) NOT NULL,
    payout_amount DECIMAL(15,2) NOT NULL,
    was_split BOOLEAN NOT NULL DEFAULT FALSE,
    was_doubled_down BOOLEAN NOT NULL DEFAULT FALSE,
    played_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_game_history_hand
        UNIQUE (round_id, user_id, hand_index),
    CONSTRAINT fk_game_history_user
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_game_history_hand CHECK (hand_index >= 0),
    CONSTRAINT chk_game_history_result
        CHECK (result IN ('PLAYER_WIN', 'DEALER_WIN', 'PUSH')),
    CONSTRAINT chk_game_history_bet CHECK (bet_amount > 0),
    CONSTRAINT chk_game_history_payout CHECK (payout_amount >= 0),
    INDEX idx_game_history_user_time (user_id, played_at)
);
