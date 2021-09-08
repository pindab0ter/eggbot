CREATE TABLE DiscordUsers
(
    discord_id                TEXT NOT NULL
        PRIMARY KEY,
    discord_tag               TEXT NOT NULL,
    inactive_until            INTEGER,
    opted_out_of_coop_lead_at INTEGER
);

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
    last_updated          INTEGER NOT NULL
);

CREATE TABLE CoopFarmers
(
    farmer TEXT    NOT NULL
        REFERENCES Farmers
            ON UPDATE CASCADE ON DELETE CASCADE,
    coop   INTEGER NOT NULL
        REFERENCES Coops
            ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE Coops
(
    id          INTEGER
        PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    contract_id TEXT NOT NULL,
    role_id     TEXT,
    leader_id   TEXT
         REFERENCES Farmers
             ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE VIEW InGameNamesToDiscordNames AS
SELECT discord_tag,
       in_game_id,
       in_game_name
FROM DiscordUsers
         JOIN Farmers ON DiscordUsers.discord_id = Farmers.discord_id
ORDER BY discord_tag;
