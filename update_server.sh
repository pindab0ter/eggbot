#!/usr/bin/env bash
cd build/install/EggBot || exit
rsync \
    --recursive \
    --checksum \
    --executability \
    --compress \
    --progress \
    --human-readable \
    -e ssh \
    ./bin \
    ./lib \
    eggbot:~/EggBot
ssh eggbot 'screen -p EggBot -X stuff $'\''^c bin/EggBot^m'\'''