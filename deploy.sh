#!/bin/bash
set -o errexit -o nounset -o pipefail
./gradlew installDist
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
  "$1":~/EggBot
ssh "$1" 'tmux send-keys -t "$2" C-c "bin/EggBot" Enter'
