#!/bin/bash
# Instala los accesos ya probados; las dependencias se describen en README.md.
set -euo pipefail
src="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
test "$(id -un)" = ale || { echo 'Este acceso está configurado para el usuario ale.' >&2; exit 1; }
test -x /home/ale/.local/opt/unity-dotnet-arm64/dotnet || { echo 'Falta .NET ARM.' >&2; exit 1; }
test -f /home/ale/.local/opt/unity-roslyn-arm64/tasks/net6.0/bincore/csc.dll || { echo 'Falta Roslyn ARM.' >&2; exit 1; }
test -f /home/ale/.local/share/unity-fex-fixes/loaders.cache || { echo 'Falta el cargador de imágenes para FEX.' >&2; exit 1; }
mkdir -p /home/ale/.local/bin /home/ale/.local/share/unity-fex-fixes /home/ale/.local/share/applications
install -m 700 "$src/abrir-unity-corregido.sh" /home/ale/.local/share/unity-fex-fixes/abrir-unity-corregido.sh
install -m 755 "$src/unity-editor-arm-experimental" /home/ale/.local/bin/unity-editor-arm-experimental
install -m 644 "$src/unity-editor-arm-experimental.desktop" /home/ale/.local/share/applications/unity-editor-arm-experimental.desktop
echo 'Acceso instalado: Unity Editor (ARM experimental).'
