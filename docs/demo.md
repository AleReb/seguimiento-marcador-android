# Demo Android 0.3

La aplicación detecta ArUco `DICT_4X4_50` con un marcador físico de **2 o 5 cm**, según el selector de la app y muestra un cubo proyectado sobre la imagen de cámara. El control de tamaño varía el lado del cubo virtual entre 1 y 20 cm; no cambia el tamaño real del marcador. La preferencia se conserva al volver a abrir la app.

El seguimiento refina las esquinas a precisión subpíxel y suaviza sus coordenadas con una constante aproximada de 65 ms. Conserva el ID seleccionado mientras siga visible, y elige el marcador de mayor área cuando debe seleccionar uno nuevo. Reinicia el filtro al cambiar de cámara, cambiar de ID o tras perder el marcador durante más de 250 ms. Descarta poses detrás de la cámara y ajustes con error de reproyección mayor de 4 px. Estos umbrales no equivalen a precisión validada y el suavizado añade algo de retardo.

## Probar

1. Abrir la carpeta `android/` en Android Studio o ejecutar `gradle -p android assembleDebug` desde la raíz del repositorio con JDK 17, Android SDK 35 y Gradle 8.9. El repositorio no incluye Gradle Wrapper.
2. Instalar el APK generado en `android/app/build/outputs/apk/debug/app-debug.apk`, abrir la app y aceptar la cámara.
3. Elegir 2 o 5 cm según el lado negro exterior impreso (sin margen blanco). Apuntar al marcador bien iluminado y comprobar que el cubo sigue las cuatro esquinas. Girar el teléfono y cambiar entre cámara frontal/trasera para comprobar el giro y espejo de la vista.
4. Variar el tamaño entre 1 y 20 cm. Comprobar el tamaño tras reiniciar la app. El cubo se oculta cuando no hay una pose válida y reaparece cuando se detecta nuevamente.
5. Para Unity, introducir la IP Wi-Fi **actual** del computador, pulsar «Enviar a Unity» y ejecutar el proyecto con el receptor actualizado en Play. Ambos dispositivos deben estar en una red que permita comunicación entre clientes. Se envían poses a UDP 5005 a un máximo de 20 Hz; no hay confirmación de entrega.
6. Mover el marcador, ocultarlo, mostrar varios IDs y alternar las cámaras durante el envío. Comprobar pérdida de señal, recuperación y ausencia de saltos entre IDs que siguen visibles.

La imagen y la proyección comparten la transformación de giro y espejo. La visualización emplea las imágenes del analizador para evitar que el cubo se dibuje sobre un fotograma distinto del usado para detectar el marcador; la tasa de refresco depende del dispositivo.

## Compatibilidad y límites

Los paquetes JSON versión 1 incluyen el campo opcional `cubeSizeCm`. El receptor Unity actualizado lo aplica como factor respecto al cubo anterior: 5 cm mantiene su escala original de 0,7 unidades. No representa tamaño físico real de una pantalla Unity. Los receptores anteriores siguen recibiendo la pose pero ignoran el tamaño.

Las distancias y la proyección aún usan intrínsecos aproximados. Consulta la [propuesta de calibración ChArUco](calibracion-camara.md). La estimación planar puede presentar ambigüedad de orientación y el seguimiento no continúa cuando el marcador queda oculto. No hay persistencia espacial ni oclusiones del mundo real.

Esta versión requiere prueba visual en teléfono, incluyendo posición del cubo, latencia, estabilidad y cámaras frontal/trasera. La comunicación UDP no está cifrada ni autenticada; úsala en una red local de confianza.
