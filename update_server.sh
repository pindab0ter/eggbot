#!/usr/bin/env bash
cd build/install/EggBot
rsync \
    --recursive \
    --checksum \
    --executability \
    --compress \
    --progress \
    --human-readable \
    --dry-run \
    -e ssh \
    ./bin \
    ./lib \
    eggbot:~/EggBot
ssh eggbot 'screen -p EggBot -X stuff $'\''^c bin/EggBot^m'\'''