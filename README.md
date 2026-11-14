# Seguimiento de marcador para Android

Proyecto para registrar el desplazamiento y la orientación de un marcador cuadrado mediante la cámara de un teléfono Android fijo.

La aplicación detectará un marcador ArUco impreso, estimará su posición y orientación respecto a la cámara y permitirá registrar el recorrido y exportar las mediciones a CSV. El tamaño físico del marcador se introducirá antes de medir.

## Documentos

- [Idea y requisitos](docs/idea-y-requisitos.md)
- [Propuesta técnica y mediciones](docs/propuesta-tecnica.md)
- [Plan de trabajo](docs/plan-de-trabajo.md)

El repositorio incluye documentación y un demo inicial de detección.

## Demo inicial

El código Kotlin de `app/` abre la cámara y busca marcadores ArUco. Consulta [cómo probar el demo](docs/demo.md) y sus limitaciones. Aún no se ha compilado ni probado en un teléfono.

## Versión web

La página estática se encuentra en [docs/index.html](docs/index.html). Detecta marcadores ArUco 4×4_50 de 5 cm, muestra una pose aproximada, permite cambiar de cámara y guardar un CSV. Para usar la cámara desde un teléfono se requiere HTTPS; GitHub Pages proporciona una dirección HTTPS. Para publicarla en GitHub Pages, subir este repositorio y seleccionar la rama principal y la carpeta `/docs` en Settings → Pages.
