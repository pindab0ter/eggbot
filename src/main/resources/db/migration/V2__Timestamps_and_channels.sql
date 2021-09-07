------------------
-- DiscordUsers --
------------------

CREATE TABLE DiscordUsers_backup
(
    discord_id                TEXT NOT NULL
        PRIMARY KEY,
    discord_tag               TEXT NOT NULL,
    inactive_until            INTEGER,
    opted_out_of_coop_lead_at INTEGER
);

INSERT INTO DiscordUsers_backup
SELECT *
FROM DiscordUsers;

DROP TABLE DiscordUsers;

CREATE TABLE DiscordUsers
(
    discord_id                TEXT    NOT NULL
        PRIMARY KEY,
    discord_tag               TEXT    NOT NULL,
    inactive_until            INTEGER,
    opted_out_of_coop_lead_at INTEGER,
    created_at                INTEGER NOT NULL,
    updated_at                INTEGER NOT NULL
);

INSERT INTO DiscordUsers
SELECT *, (STRFTIME('%s', 'now') * 1000), (STRFTIME('%s', 'now') * 1000)
FROM DiscordUsers_backup;

DROP TABLE DiscordUsers_backup;



-------------
-- Farmers --
-------------

CREATE TABLE Farmers_backup
(
    in_game_id            TEXT    NOT NULL
        PRIMARY KEY,
    discord_id            TEXT    NOT NULL
        REFERENCES DiscordUsers
            ON DELETE CASCADE,
    in_game_name          TEXT    NOT NULL,
    prestiges             BIGINT  NOT NULL,
    soul_eggs             REAL    NOT NULL,
    soul_bonus            INTEGER NOT NULL,
    prophecy_eggs         BIGINT  NOT NULL,
    prophecy_bonus        INTEGER NOT NULL,
    drone_takedowns       INTEGER NOT NULL,
    elite_drone_takedowns INTEGER NOT NULL,
    last_updated          INTEGER NOT NULL
);

INSERT INTO Farmers_backup
SELECT *
FROM Farmers;

DROP TABLE Farmers;

CREATE TABLE Farmers
(
    in_game_id            TEXT    NOT NULL
        PRIMARY KEY,
    discord_id            TEXT    NOT NULL
        REFERENCES DiscordUsers
            ON DELETE CASCADE,
    in_game_name          TEXT    NOT NULL,
    prestiges             BIGINT  NOT NULL,
    soul_eggs             REAL    NOT NULL,
    soul_bonus            INTEGER NOT NULL,
    prophecy_eggs         BIGINT  NOT NULL,
    prophecy_bonus        INTEGER NOT NULL,
    drone_takedowns       INTEGER NOT NULL,
    elite_drone_takedowns INTEGER NOT NULL,
    created_at            INTEGER NOT NULL,
    updated_at            INTEGER NOT NULL
);

INSERT INTO Farmers(in_game_id, discord_id, in_game_name, prestiges, soul_eggs, soul_bonus, prophecy_eggs,
                    prophecy_bonus, drone_takedowns, elite_drone_takedowns, created_at, updated_at)
SELECT in_game_id,
       discord_id,
       in_game_name,
       prestiges,
       soul_eggs,
       soul_bonus,
       prophecy_eggs,
       prophecy_bonus,
       drone_takedowns,
       elite_drone_takedowns,
       (STRFTIME('%s', 'now') * 1000),
       last_updated
FROM Farmers_backup;

DROP TABLE Farmers_backup;



-----------
-- Coops --
-----------

CREATE TABLE Coops_backup
(
    id          INT
        PRIMARY KEY,
    name        TEXT NOT NULL,
    contract_id TEXT NOT NULL,
    role_id     TEXT,
    leader_id   TEXT
         REFERENCES Farmers
             ON UPDATE CASCADE ON DELETE SET NULL
);

INSERT INTO Coops_backup
SELECT *
FROM Coops;

DROP TABLE Coops;

CREATE TABLE Coops
(
    id          INT
        PRIMARY KEY,
    name        TEXT    NOT NULL,
    contract_id TEXT    NOT NULL,
    leader_id   TEXT
        REFERENCES Farmers
            ON UPDATE CASCADE ON DELETE SET NULL,
    role_id     TEXT,
    channel_id  TEXT,
    created_at  INTEGER NOT NULL,
    updated_at  INTEGER NOT NULL
);

INSERT INTO Coops (id, name, contract_id, leader_id, role_id, channel_id, created_at, updated_at)
SELECT id,
       name,
       contract_id,
       leader_id,
       role_id,
       NULL,
       (STRFTIME('%s', 'now') * 1000),
       (STRFTIME('%s', 'now') * 1000)
FROM Coops_backup;

DROP TABLE Coops_backup;
