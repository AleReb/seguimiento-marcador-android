# Demo de detección

Esta primera versión muestra la cámara trasera y avisa cuando detecta un marcador del diccionario ArUco `DICT_4X4_50`, junto con su ID. Aún no calcula distancia, desplazamiento ni orientación; tampoco guarda registros ni dibuja un contorno. El proyecto no se ha compilado ni probado en un teléfono.

## Probar

1. Abrir el proyecto en Android Studio en un equipo compatible, con JDK 17, Android SDK 35 y Gradle 8.9 (requerido por Android Gradle Plugin 8.7.3). El proyecto todavía no incluye Gradle Wrapper. También se puede compilar con `gradle assembleDebug` si esas herramientas están instaladas y configuradas.
2. Conectar un teléfono Android 6.0 o posterior con depuración USB habilitada y ejecutar la aplicación; aceptar el permiso de cámara.
3. Mostrar a la cámara un marcador del diccionario `DICT_4X4_50`, impreso o en otra pantalla, rodeado de un margen blanco. El ID detectado aparecerá bajo la imagen.

La primera ejecución requiere descargar dependencias de Google Maven y Maven Central. Este equipo usa Linux ARM, plataforma que Android Studio no soporta oficialmente; todavía hay que investigar y preparar una forma viable de compilar aquí.
