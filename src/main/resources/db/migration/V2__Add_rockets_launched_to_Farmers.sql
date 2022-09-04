CREATE TABLE farmers_new
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
    rockets_launched      INTEGER   NOT NULL,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO farmers_new
SELECT egg_inc_id
     , discord_user_id
     , in_game_name
     , soul_eggs
     , soul_bonus
     , prophecy_eggs
     , prophecy_bonus
     , prestiges
     , drone_takedowns
     , elite_drone_takedowns
     , 0
     , created_at
     , updated_at
FROM farmers;

DROP TABLE farmers CASCADE;

ALTER TABLE farmers_new
    RENAME TO farmers;
