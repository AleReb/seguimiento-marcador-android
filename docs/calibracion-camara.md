# Propuesta de calibración de cámara

La versión 0.3 aún supone un campo visual horizontal de 60° y distorsión cero. El suavizado y la proyección nueva no sustituyen una calibración. El error de ajuste mostrado en píxeles tampoco mide la exactitud real de las distancias.

## Hojas listas para imprimir

- [Tablero ChArUco A4](calibracion/hoja-charuco-a4.pdf): 5 × 7 cuadrados de 30 mm, marcadores de 15 mm, `DICT_4X4_50`. Tablero completo de 150 × 210 mm.
- [Marcador ArUco ID 0 de 5 cm](calibracion/marcador-aruco-5cm-a4.pdf): para probar el seguimiento actual, distinto del tablero de calibración.

Imprimir al 100 %, sin ajustar a página. Comprobar con una regla que la línea de control mide 100 mm y el lado de un cuadrado del tablero mide 30 mm. Pegar la hoja plana sobre una base rígida. Se pueden regenerar los PDF con `tools/generar-hojas.py` (OpenCV contrib 4.10, Pillow y ReportLab).

## Procedimiento propuesto

1. Imprimir un tablero ChArUco de OpenCV sobre una superficie plana y rígida, a escala 100 %, sin «ajustar a página». Medir con regla el lado real de los cuadrados y de los marcadores. Se puede usar un tablero 5 × 7 con cuadrados de 30 mm y marcadores de 15 mm; el tablero generado y el detector deben compartir las mismas dimensiones y diccionario. Este tablero de calibración es distinto del marcador individual de 5 cm usado para el seguimiento.
2. Usar la misma cámara, resolución de análisis, zoom y orientación de sensor que se utilizarán en la aplicación. Calibrar por separado cámara frontal y trasera. Evitar cambios de zoom o lente; fijar el enfoque para calibrar si el dispositivo lo permite.
3. Como punto de partida, recoger 15–25 imágenes nítidas, separadas entre sí: tablero de frente e inclinado, cerca y lejos, en el centro y cerca de cada borde. Mantener el tablero quieto durante cada captura. Esa cantidad es una propuesta de trabajo; la cobertura y la calidad importan más que el número.
4. Detectar esquinas ChArUco y resolver los parámetros intrínsecos y la distorsión con OpenCV. Revisar el error de reproyección total y por imagen; descartar imágenes borrosas y repetir vistas que falten. Reservar imágenes adicionales para validar, sin incluirlas en el cálculo.
5. Guardar `fx`, `fy`, `cx`, `cy`, coeficientes de distorsión, dimensiones de imagen, identificador de cámara/lente, zoom, dimensiones del tablero y error de reproyección. La futura app debería cargar un perfil solo si coincide con la cámara y resolución activas, mostrar «Calibrada» o «Aproximada» y permitir borrarlo/repetirlo.
6. Usar la misma matriz y distorsión en `solvePnP` y `projectPoints`, manteniendo las coordenadas del sensor. El giro y espejo de la vista deben aplicarse únicamente al dibujar la imagen y la proyección.
7. Validar con un marcador de tamaño medido a varias distancias conocidas y en los bordes de imagen. Registrar error de distancia y estabilidad; no prometer precisión basándose únicamente en un error de reproyección bajo.

## Alcance actual

Esto es una propuesta: aún no hay pantalla de captura de calibración, cálculo ni importación de perfiles en la app. Para implementarlo conviene añadir un asistente «Calibrar cámara» con contador de vistas, indicaciones de cobertura, resultado de calidad y guardado del perfil; hasta entonces todas las poses se presentan como estimaciones sin calibrar.

Referencia: [OpenCV 4.10 — calibración con ArUco y ChArUco](https://docs.opencv.org/4.10.0/da/d13/tutorial_aruco_calibration.html).

## Alternativa en papel Carta

El [PDF Carta](calibracion/hoja-charuco-carta.pdf) conserva los cuadrados de 30 mm y los marcadores de 15 mm; no es una reducción del A4. También hay [ArUco de 2 cm](calibracion/marcadores-aruco-2cm-carta.pdf) para seguimiento. La app 0.3 permite seleccionar el tamaño físico de 2 o 5 cm por separado del tamaño virtual del cubo.
