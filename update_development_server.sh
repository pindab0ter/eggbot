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
  1eHans-NUC:~/TestEggBot
ssh 1eHans-NUC 'tmux send-keys -t TestEggBot C-c "bin/EggBot" Enter'
