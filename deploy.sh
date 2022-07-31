#!/bin/bash
set -o errexit -o nounset -o pipefail

# Usage
# $ ./deploy.sh ssh-config-name folder-and-pane-name
# * ssh-config-name: The name of the SSH config set up to connect to the server to deploy to
# * name: The name of the folder in the home directory and tmux window (must be the same)

./gradlew installDist
cd build/install/EggBot
SSH_CONFIG_NAME=$1
NAME=$2

echo "Updating ${NAME} on ${SSH_CONFIG_NAME}…"
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
  "$SSH_CONFIG_NAME":~/"$NAME"

# shellcheck disable=SC2087
ssh -t "$SSH_CONFIG_NAME" <<EOF
  pkill -f /$NAME
  tmux new-window -n $NAME -c $NAME bin/EggBot
EOF
