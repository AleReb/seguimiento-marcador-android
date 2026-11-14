# Idea y requisitos

## Objetivo

Con un teléfono Android apoyado en una posición fija, observar un marcador cuadrado impreso y registrar cómo se mueve y gira a lo largo del tiempo. La cámara del teléfono será el único sensor necesario para la medición.

## Uso previsto

1. Imprimir un marcador compatible y medir el lado exterior del cuadrado en milímetros.
2. Fijar el teléfono con la cámara apuntando al área de movimiento.
3. Abrir la aplicación, permitir el uso de la cámara e introducir el tamaño del marcador.
4. Situar el marcador visible y establecer su posición inicial como cero.
5. Iniciar el registro, mover el marcador y detenerlo al terminar.
6. Revisar el recorrido y exportar las mediciones.

## Primera versión

- Vista de cámara con el marcador detectado resaltado.
- Configuración del tamaño y selección o identificación del marcador.
- Posición en tres ejes, distancia a la cámara y orientación estimadas.
- Botón para establecer el origen inicial y controles para iniciar y detener el registro.
- Visualización sencilla del recorrido y estado de detección.
- Exportación CSV con tiempos, posición, orientación y estado de detección.
- Aviso cuando el marcador no se detecte; no inventar posiciones durante esos intervalos.

## Condiciones y límites

El teléfono debe permanecer fijo y el marcador debe estar visible, suficientemente nítido e iluminado. Las medidas son estimaciones: dependen del tamaño real introducido, la calibración de cámara, la distancia, el ángulo y la resolución. La precisión se determinará con pruebas reales; no se promete una precisión específica. Mover el teléfono cambia la referencia. Si el marcador sale de la imagen, su movimiento durante ese intervalo no se puede medir con esta propuesta.

## Pendiente de definir

- Modelo y versión Android del teléfono y rango de distancias esperado.
- Tamaño del área de movimiento y precisión necesaria.
- Forma preferida de mostrar el recorrido y frecuencia de registro deseada.
