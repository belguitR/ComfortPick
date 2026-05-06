CREATE TABLE riot_accounts (
    id UUID PRIMARY KEY,
    puuid TEXT NOT NULL UNIQUE,
    game_name TEXT NOT NULL,
    tag_line TEXT NOT NULL,
    region TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ux_riot_accounts_region_game_tag
    ON riot_accounts (region, game_name, tag_line);

CREATE INDEX ix_riot_accounts_puuid
    ON riot_accounts (puuid);

CREATE TABLE matches (
    id UUID PRIMARY KEY,
    riot_match_id TEXT NOT NULL UNIQUE,
    region TEXT NOT NULL,
    queue_id INT,
    game_creation TIMESTAMP NOT NULL,
    game_duration_seconds INT NOT NULL,
    patch TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX ix_matches_riot_match_id
    ON matches (riot_match_id);

CREATE TABLE player_matchups (
    id UUID PRIMARY KEY,
    riot_account_id UUID NOT NULL REFERENCES riot_accounts (id),
    match_id UUID NOT NULL REFERENCES matches (id),
    user_puuid TEXT NOT NULL,
    user_champion_id INT NOT NULL,
    enemy_champion_id INT NOT NULL,
    role TEXT NOT NULL,
    win BOOLEAN NOT NULL,
    kills INT NOT NULL,
    deaths INT NOT NULL,
    assists INT NOT NULL,
    total_cs INT,
    gold_earned INT,
    total_damage_to_champions INT,
    item_0 INT,
    item_1 INT,
    item_2 INT,
    item_3 INT,
    item_4 INT,
    item_5 INT,
    item_6 INT,
    primary_rune_id INT,
    secondary_rune_id INT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT ux_player_matchups_account_match UNIQUE (riot_account_id, match_id)
);

CREATE INDEX ix_player_matchups_match_id
    ON player_matchups (match_id);

CREATE INDEX ix_player_matchups_user_puuid
    ON player_matchups (user_puuid);

CREATE INDEX ix_player_matchups_user_champion_role
    ON player_matchups (user_champion_id, role);

CREATE TABLE personal_matchup_stats (
    id UUID PRIMARY KEY,
    riot_account_id UUID NOT NULL REFERENCES riot_accounts (id),
    enemy_champion_id INT NOT NULL,
    user_champion_id INT NOT NULL,
    role TEXT NOT NULL,
    games INT NOT NULL,
    wins INT NOT NULL,
    losses INT NOT NULL,
    winrate DOUBLE PRECISION NOT NULL,
    average_kda DOUBLE PRECISION,
    average_cs DOUBLE PRECISION,
    average_gold DOUBLE PRECISION,
    average_damage DOUBLE PRECISION,
    personal_score DOUBLE PRECISION NOT NULL,
    confidence TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ux_personal_matchup_stats_lookup UNIQUE (riot_account_id, enemy_champion_id, user_champion_id, role)
);

CREATE INDEX ix_personal_matchup_stats_account_enemy
    ON personal_matchup_stats (riot_account_id, enemy_champion_id);

CREATE INDEX ix_personal_matchup_stats_account_enemy_user
    ON personal_matchup_stats (riot_account_id, enemy_champion_id, user_champion_id);

CREATE TABLE personal_build_stats (
    id UUID PRIMARY KEY,
    riot_account_id UUID NOT NULL REFERENCES riot_accounts (id),
    enemy_champion_id INT NOT NULL,
    user_champion_id INT NOT NULL,
    role TEXT NOT NULL,
    item_sequence TEXT NOT NULL,
    games INT NOT NULL,
    wins INT NOT NULL,
    winrate DOUBLE PRECISION NOT NULL,
    build_score DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ux_personal_build_stats_lookup UNIQUE (riot_account_id, enemy_champion_id, user_champion_id, role, item_sequence)
);
