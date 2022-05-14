CREATE TABLE discord_guilds
(
    id         TEXT      NOT NULL PRIMARY KEY,
    name       TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE discord_users
(
    id             TEXT      NOT NULL PRIMARY KEY,
    tag            TEXT      NOT NULL,
    inactive_until TIMESTAMP,
    guild_id       TEXT      NOT NULL
        REFERENCES discord_guilds
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE farmers
(
    egg_inc_id            TEXT      NOT NULL PRIMARY KEY,
    discord_id            TEXT      NOT NULL
        REFERENCES discord_users
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    in_game_name          TEXT      NOT NULL,
    soul_eggs             REAL      NOT NULL,
    soul_bonus            INTEGER   NOT NULL,
    prophecy_eggs         BIGINT    NOT NULL,
    prophecy_bonus        INTEGER   NOT NULL,
    prestiges             BIGINT    NOT NULL,
    drone_takedowns       INTEGER   NOT NULL,
    elite_drone_takedowns INTEGER   NOT NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE coops
(
    id          INTEGER PRIMARY KEY,
    name        TEXT      NOT NULL,
    contract_id TEXT      NOT NULL,
    role_id     TEXT,
    channel_id  TEXT,
    guild_id    TEXT      NOT NULL
        REFERENCES discord_guilds
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE coop_farmers
(
    farmer TEXT    NOT NULL
        REFERENCES farmers
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    coop   INTEGER NOT NULL
        REFERENCES coops
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

CREATE VIEW names AS
SELECT tag                 AS discord_tag,
       egg_inc_id,
       in_game_name,
       discord_guilds.name AS guild_name
FROM discord_users
         JOIN farmers ON discord_users.id = farmers.discord_id
         JOIN discord_guilds ON discord_users.guild_id = discord_guilds.id
ORDER BY tag;
