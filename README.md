# Seguimiento de marcador para Android

Proyecto para detectar un marcador ArUco, estimar su posición y orientación respecto a una cámara y visualizar sus movimientos. Incluye una demo Android, una versión web y una integración experimental con Unity.

## Qué hay disponible

- **Android:** la aplicación en `app/` muestra la cámara, detecta marcadores `DICT_4X4_50` y estima posición, distancia y orientación a partir de un marcador de 5 cm. Permite cambiar entre cámaras y enviar la pose por UDP a Unity.
- **Web:** la demo en [docs/index.html](docs/index.html) detecta marcadores ArUco 4×4, muestra una pose aproximada, permite cambiar de cámara y guardar mediciones en CSV.
- **Unity:** el componente [MarcadorDemo.cs](integracion-unity/MarcadorDemo.cs) recibe las poses UDP en el puerto 5005 y mueve un cubo. También se incluye el proyecto mínimo `integracion-unity/CuboMinimo/`.
- **APK:** [demo-marcador-udp.apk](apk/demo-marcador-udp.apk), almacenado mediante Git LFS.

## Límites y estado

Las estimaciones de pose de las demos Android y web usan parámetros aproximados de cámara, no una calibración real; los resultados no representan precisión validada. El tamaño del marcador está fijado en 5 cm. La demo Android transmite la pose, pero no ofrece registro CSV; esa función está en la versión web.

La transmisión UDP no está cifrada ni autenticada y debe usarse solo en una red local de confianza. La integración con Unity y el editor ARM son experimentales; consulta [el resultado de la prueba de Unity en ARM](integracion-unity/RESULTADO-UNITY-ARM.md). El proyecto Android aún requiere pruebas en un teléfono.

## Documentación

- [Demo Android: instalación, ejecución y limitaciones](docs/demo.md)
- [Demo Unity: conexión UDP y uso](integracion-unity/README.md)
- [Idea y requisitos](docs/idea-y-requisitos.md)
- [Propuesta técnica y mediciones](docs/propuesta-tecnica.md)
- [Plan de trabajo](docs/plan-de-trabajo.md)

## Publicar la versión web

La página estática se encuentra en [docs/index.html](docs/index.html). Para usar la cámara desde un teléfono se requiere HTTPS; GitHub Pages proporciona una dirección HTTPS. Para publicarla, selecciona la rama principal y la carpeta `/docs` en Settings → Pages.
