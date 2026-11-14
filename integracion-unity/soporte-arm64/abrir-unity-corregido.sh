#!/bin/bash
set -u
original=$(cat /proc/sys/vm/overcommit_memory)
base=/home/ale/Unity/Hub/Editor/6000.3.28f1/Editor/Data/NetCoreRuntime
backup="$base/dotnet.original-arm-test"
[ ! -e "$backup" ] || { echo 'Ya hay otra prueba activa.'; exit 1; }
restore() { [ ! -e "$backup" ] || mv -f "$backup" "$base/dotnet"; printf '%s\n' "$original" > /proc/sys/vm/overcommit_memory; }
trap restore EXIT
trap 'exit 130' HUP INT TERM
mv "$base/dotnet" "$backup" || exit 1
cat > "$base/dotnet" <<'WRAPPER'
#!/bin/bash
if [ "${1:-}" = exec ] && [[ "${2:-}" = */DotNetSdkRoslyn/csc.dll ]]; then
  shift 2
  exec /home/ale/.local/opt/unity-dotnet-arm64/dotnet exec /home/ale/.local/opt/unity-roslyn-arm64/tasks/net6.0/bincore/csc.dll "$@"
fi
exec /home/ale/.local/bin/FEX /home/ale/Unity/Hub/Editor/6000.3.28f1/Editor/Data/NetCoreRuntime/dotnet.original-arm-test "$@"
WRAPPER
chmod 755 "$base/dotnet"
printf '1\n' > /proc/sys/vm/overcommit_memory || exit 1
runuser -u ale -- env HOME=/home/ale USER=ale LOGNAME=ale PATH=/home/ale/.local/bin:/usr/local/bin:/usr/bin:/bin DISPLAY=:0 WAYLAND_DISPLAY=wayland-1 XDG_RUNTIME_DIR=/run/user/1000 DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/1000/bus GDK_BACKEND=wayland,x11 GDK_PIXBUF_MODULE_FILE=/home/ale/.local/share/unity-fex-fixes/loaders.cache LD_LIBRARY_PATH=/home/ale/.local/share/unity-fex-fixes DOTNET_EnableWriteXorExecute=0 /home/ale/.local/bin/FEX /home/ale/Unity/Hub/Editor/6000.3.28f1/Editor/Unity -diag-debug-shader-compiler -job-worker-count 1 -projectPath /home/ale/Documents/repos/seguimiento-marcador-android/integracion-unity/CuboMinimo -logFile /tmp/unity-demo-visual-arm.log
result=$?
printf 'Unity terminó con código %s.\n' "$result"
exit "$result"
