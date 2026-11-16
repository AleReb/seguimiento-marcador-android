# Seguimiento de marcador para Android

Proyecto para detectar un marcador ArUco, estimar su posición y orientación respecto a una cámara y visualizar sus movimientos. Incluye una demo Android, una versión web y una integración experimental con Unity.

## Qué hay disponible

- **Android:** la aplicación en `app/` muestra la cámara, detecta marcadores `DICT_4X4_50` y estima posición, distancia y orientación a partir de un marcador de 2 o 5 cm (seleccionable). Permite cambiar entre cámaras, proyectar un cubo sobre el marcador, variar su tamaño (1–20 cm) y enviar la pose y tamaño por UDP a Unity. El seguimiento añade refinamiento subpíxel, suavizado de esquinas y selección estable del ID visible.
- **Web:** la demo en [docs/index.html](docs/index.html) detecta marcadores ArUco 4×4, muestra una pose aproximada, permite cambiar de cámara y guardar mediciones en CSV.
- **Unity:** el componente [MarcadorDemo.cs](integracion-unity/MarcadorDemo.cs) recibe las poses UDP en el puerto 5005 y mueve un cubo. También se incluye el proyecto mínimo `integracion-unity/CuboMinimo/`.
- **APK:** [demo-marcador-udp.apk](apk/demo-marcador-udp.apk), almacenado mediante Git LFS.

## Límites y estado

Las estimaciones de pose de las demos Android y web usan parámetros aproximados de cámara, no una calibración real; los resultados no representan precisión validada. El selector debe coincidir con el lado impreso de 2 o 5 cm; de lo contrario las distancias serán incorrectas. La demo Android transmite la pose, pero no ofrece registro CSV; esa función está en la versión web.

La transmisión UDP no está cifrada ni autenticada y debe usarse solo en una red local de confianza. La integración con Unity y el editor ARM son experimentales; consulta [el resultado de la prueba de Unity en ARM](integracion-unity/RESULTADO-UNITY-ARM.md). La versión Android 0.3 aún requiere pruebas visuales en un teléfono. El APK de `apk/` corresponde a la versión publicada anteriormente; la versión nueva se guarda aparte como [demo-marcador-v0.3.apk](apk/demo-marcador-v0.3.apk).

## Documentación

- [Demo Android: instalación, ejecución y limitaciones](docs/demo.md)
- [Demo Unity: conexión UDP y uso](integracion-unity/README.md)
- [Hoja ChArUco A4 para calibración](docs/calibracion/hoja-charuco-a4.pdf)
- [Marcador ArUco de 5 cm para seguimiento](docs/calibracion/marcador-aruco-5cm-a4.pdf)
- [Propuesta de calibración de cámara](docs/calibracion-camara.md)
- [Idea y requisitos](docs/idea-y-requisitos.md)
- [Propuesta técnica y mediciones](docs/propuesta-tecnica.md)
- [Plan de trabajo](docs/plan-de-trabajo.md)

## Publicar la versión web

La página estática se encuentra en [docs/index.html](docs/index.html). Para usar la cámara desde un teléfono se requiere HTTPS; GitHub Pages proporciona una dirección HTTPS. Para publicarla, selecciona la rama principal y la carpeta `/docs` en Settings → Pages.

## Hojas en tamaño Carta

Imprimir al **100 %**, sin ajustar a página. Comprobar con regla la línea de 100 mm.

- [Tablero ChArUco de calibración](docs/calibracion/hoja-charuco-carta.pdf).
- [Marcadores ArUco de 2 cm, IDs 0–5](docs/calibracion/marcadores-aruco-2cm-carta.pdf).
- [Cubo de papel de 2 cm](docs/calibracion/cubo-papel-2cm-carta.pdf).
- [Cubo de papel de 5 cm](docs/calibracion/cubo-papel-5cm-carta.pdf).

Los cubos son plantillas para recortar y armar; sus caras no incluyen marcadores. Un ArUco de 2 cm necesita margen blanco, por lo que no cabe con ese margen en una cara de cubo de exactamente 2 cm. Puede pegarse en una cara de 5 cm o mantenerse en una superficie plana independiente. La calibración mediante ChArUco sigue siendo una propuesta; su asistente no está implementado.

Consulta las [notas de la versión 0.3](docs/version-0.3.md).
