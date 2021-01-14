#!/bin/bash

# Usage
# $ ./deploy.sh ssh-config-name folder-and-pane-name
# * ssh-config-name: The name of the SSH config set up to connect to the server to deploy to
# * name: The name of the folder in the home directory and tmux window (must be the same)

set -o errexit -o nounset -o pipefail
./gradlew installDist
cd build/install/EggBot
ssh_config=$1
name=$2
echo "Updating ${name} on ${ssh_config}…"
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
  "$ssh_config":~/"$name"
# shellcheck disable=SC2087
ssh -t "$ssh_config" <<EOF
  pkill -f /$name
  tmux new-window -n $name -c $name bin/EggBot
EOF
