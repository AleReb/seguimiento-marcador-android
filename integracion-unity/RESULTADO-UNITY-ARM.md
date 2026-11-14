# Prueba de Unity en Linux ARM

Unity Editor 6000.3.28f1 corre mediante FEX 2609. El compilador Roslyn incluido con Unity falla bajo la emulación; además, la política automática de memoria impide iniciar ciertos procesos pese a quedar RAM disponible.

El 13 de noviembre de 2026 se realizó una prueba reversible con el proyecto `CuboMinimo`: se utilizó temporalmente `vm.overcommit_memory=1`, .NET Runtime 6.0.36 nativo para ARM y Microsoft Roslyn 4.3.1 nativo para compilar C#. La compilación de Unity terminó con `Tundra build success` y la importación finalizó con `Exiting batchmode successfully now!` y código 0. El registro de esa prueba está en `/tmp/unity-prueba-roslyn-arm.log` mientras exista.

Al finalizar, el ejecutable original `Editor/Data/NetCoreRuntime/dotnet` y la política original `vm.overcommit_memory=0` se restauraron y verificaron. .NET ARM y Roslyn de prueba se conservaron en `~/.local/opt/unity-dotnet-arm64` y `~/.local/opt/unity-roslyn-arm64`.

La prueba solo demuestra compilación e importación en modo sin interfaz: falta crear un lanzador seguro y verificar que el editor gráfico, el script del cubo y la conexión con el celular funcionen. El cambio de política es global mientras está activo; requiere precaución y debe restaurarse al cerrar Unity. La combinación continúa siendo experimental y no está soportada oficialmente.

## Acceso experimental

El acceso del menú «Unity Editor (ARM experimental)» ahora inicia `CuboMinimo` con el compilador ARM probado. Requiere autenticación de administrador para habilitar temporalmente la política de memoria; al cerrar el editor, el lanzador intenta restaurar el ejecutable original y la política anterior. No se debe iniciar una segunda instancia al mismo tiempo. Si el proceso se termina de manera forzada, comprobar la restauración antes de iniciar de nuevo.
