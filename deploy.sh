#!/bin/bash

# Usage
#  $ ./deploy.sh ssh-config-name folder-and-pane-name
# * ssh-config-name: The name of the SSH config set up to connect to the server to deploy to
# * folder-and-pane-name: The name of the folder in the home directory and tmux pane (must be the same)

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
  "$1":~/"$2"
ssh "$1" 'tmux send-keys -t "$2" C-c "bin/EggBot" Enter'
