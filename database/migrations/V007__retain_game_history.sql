ALTER TABLE game_history
    DROP FOREIGN KEY fk_game_history_user,
    MODIFY user_id BIGINT NULL,
    ADD COLUMN leaderboard_id BIGINT NULL,
    ADD CONSTRAINT fk_history_active_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_game_history_leaderboard FOREIGN KEY (leaderboard_id) REFERENCES leaderboard(id),
    ADD CONSTRAINT uq_game_history_run_hand UNIQUE (round_id, leaderboard_id, hand_index);

UPDATE game_history h JOIN leaderboard l ON l.user_id = h.user_id
SET h.leaderboard_id = l.id;
