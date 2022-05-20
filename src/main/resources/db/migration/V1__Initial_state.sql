CREATE TABLE discord_users
(
    id             TEXT      NOT NULL PRIMARY KEY,
    tag            TEXT      NOT NULL,
    inactive_until TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE farmers
(
    egg_inc_id            TEXT      NOT NULL PRIMARY KEY,
    discord_user_id       TEXT      NOT NULL
        REFERENCES discord_users
            ON UPDATE CASCADE
            ON DELETE CASCADE,
    in_game_name          TEXT,
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
    id                SERIAL PRIMARY KEY,
    name              TEXT      NOT NULL,
    contract_id       TEXT      NOT NULL,
    role_id           TEXT,
    channel_id        TEXT,
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (name, contract_id)
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

CREATE VIEW farmers_to_discord_users AS
    SELECT discord_users.tag AS discord_tag
         , farmers.egg_inc_id
         , farmers.in_game_name
         , CASE
               WHEN discord_users.inactive_until < now() OR discord_users.inactive_until IS NULL
                   THEN TRUE
                   ELSE FALSE
           END               AS active
    FROM farmers
             JOIN discord_users ON discord_users.id = farmers.discord_user_id
    ORDER BY discord_users.tag;