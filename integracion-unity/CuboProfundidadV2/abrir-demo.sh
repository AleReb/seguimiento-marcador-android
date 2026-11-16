#!/usr/bin/env bash
set -euo pipefail
project=$(cd -- "$(dirname -- "$0")" && pwd)
launcher="$HOME/.local/bin/unity-editor-arm-experimental"
if [[ ! -x "$launcher" ]]; then
  echo 'Instala primero el lanzador ARM del repositorio unity-raspberry-pi.' >&2
  exit 1
fi
exec "$launcher" "$project"
