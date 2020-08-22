#!/usr/bin/env bash
set -o errexit -o nounset -o pipefail
gradle installDist
cd build/install/EggBot
rsync \
  --recursive \
  --checksum \
  --executability \
  --delete \
  --compress \
  --progress \
  --human-readable \
  -e ssh \
  ./bin \
  ./lib \
  EggBot:~/EggBot
ssh EggBot 'screen -p EggBot -X stuff $'\''^c bin/EggBot^m'\'''
