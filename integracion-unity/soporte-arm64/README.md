# Correcciones de Unity para este equipo ARM

Estos archivos documentan y permiten volver a instalar el acceso **Unity Editor (ARM experimental)**, probado en el equipo de Ale con Unity 6000.3.28f1, FEX 2609 y el proyecto `CuboMinimo`. Las rutas y el usuario están fijados a `/home/ale`; se deben adaptar para otra máquina. Unity para Linux ARM mediante FEX no está soportado oficialmente.

- `unity-editor-arm-experimental` evita abrir dos instancias e inicia el asistente con `pkexec`.
- `abrir-unity-corregido.sh` ejecuta Unity mediante FEX, sustituye temporalmente su lanzador .NET por uno que deriva `csc.dll` a Roslyn nativo ARM, y usa un solo proceso de shaders y un trabajador. Restaura el ejecutable y la política de memoria al cerrarse normalmente.
- `unity-editor-arm-experimental.desktop` agrega el acceso al menú; `instalar-acceso.sh` reinstala estos tres archivos si ya existen las dependencias.

El asistente solicita la contraseña de administrador porque cambia `vm.overcommit_memory` a 1 **para todo el sistema durante la sesión**. Con esa política Linux permite reservas de memoria virtual más grandes y, si la memoria física realmente se agota, puede terminar procesos. Al cerrar Unity, el asistente vuelve al valor anterior. Un apagado brusco o una terminación forzada del asistente puede impedir la limpieza; el valor de memoria se reinicia al reiniciar el computador, pero el archivo .NET requiere revisión manual.

Si queda una prueba interrumpida, con Unity cerrado comprueba `cat /proc/sys/vm/overcommit_memory`. Si existe `~/Unity/Hub/Editor/6000.3.28f1/Editor/Data/NetCoreRuntime/dotnet.original-arm-test`, verifica que ningún Unity o asistente esté activo antes de restaurarlo como `dotnet`; el respaldo es el ejecutable original. No borres el respaldo ni lo sustituyas si hay una sesión activa.

## Dependencias utilizadas

- Unity 6000.3.28f1 y FEX 2609 con RootFS Fedora 43.
- .NET Runtime 6.0.36 ARM en `~/.local/opt/unity-dotnet-arm64`, instalado con el script oficial `https://dot.net/v1/dotnet-install.sh` usando `--version 6.0.36 --runtime dotnet --architecture arm64 --no-path`.
- Microsoft.Net.Compilers.Toolset 4.3.1 de `https://api.nuget.org/v3-flatcontainer/microsoft.net.compilers.toolset/4.3.1/microsoft.net.compilers.toolset.4.3.1.nupkg`, SHA-256 `ccbb7f75ba7271f5fad020e8b1b4eeffe5c56da5dd3a17797c2943aacf29ef78`; extraer `tasks/net6.0/bincore/` en `~/.local/opt/unity-roslyn-arm64/`.
- Los cargadores SVG y pixbuf x86-64 extraídos de `rsvg-pixbuf-loader-2.60.0-1.fc42.x86_64.rpm` (SHA-256 `c34883d029d6fddeb26c7c92d08a18c5f833e39fe9727f16a594117d27896c4d`) y `gdk-pixbuf2-2.42.12-10.fc42.x86_64.rpm` (SHA-256 `d02b522b9ec30d4c71122943a9cb2090db0a78966bea3e6cdccbe50ebcb04489`), en `~/.local/share/unity-fex-fixes/`. `loaders.cache` se generó con el `gdk-pixbuf-query-loaders-64` del RootFS, invocado mediante FEX, apuntando al cargador SVG extraído. Estos componentes son de Fedora y mantienen sus licencias originales.

Consulta también [el resultado de la prueba](../RESULTADO-UNITY-ARM.md) y [las instrucciones del demo](../README.md). Las teclas se atienden en la pestaña Game al entrar en Play: flechas para X/Y, W/S para profundidad y Q/E para giro.
