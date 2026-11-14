# Propuesta técnica

## Componentes

- Aplicación Android nativa, previsiblemente con Kotlin y CameraX para obtener imágenes de la cámara.
- OpenCV con detección ArUco y estimación de pose a partir de las cuatro esquinas, el tamaño conocido del marcador y los parámetros de la cámara. La integración y las versiones concretas se comprobarán durante el desarrollo.
- Procesamiento local de imágenes y almacenamiento local de los registros. El usuario elegirá dónde guardar el CSV mediante el selector de archivos de Android.

## Sistema de referencia

La pose se calcula respecto a la cámara fija. Como convención inicial, X apunta a la derecha de la imagen, Y hacia abajo y Z hacia delante desde la cámara. Se debe verificar la convención al integrar la biblioteca y la rotación de la imagen. La posición será la del centro del marcador; se expresará en milímetros y podrá mostrarse en centímetros.

Al pulsar «establecer cero» se guarda la posición inicial. El desplazamiento es la posición actual menos esa posición inicial, en los ejes de la cámara. La profundidad es Z; la distancia a la cámara es la distancia tridimensional al centro, √(X² + Y² + Z²). La orientación se calculará con la rotación del marcador respecto a la cámara; para una orientación relativa inicial se combinarán las rotaciones, no se restarán ángulos directamente. La presentación de ángulos y su convención se definirá y comprobará durante la implementación.

## Calibración

La estimación necesita parámetros intrínsecos y distorsión de la cámara. Se investigará la disponibilidad de parámetros del dispositivo y se ofrecerá calibración con un patrón conocido cuando sea necesario. Si se usan valores aproximados, la interfaz deberá indicarlo. El tamaño introducido corresponde al lado exterior del cuadrado negro del marcador, sin incluir el margen blanco.

## Registro

Cada muestra detectada incluirá tiempo transcurrido, posición absoluta respecto a la cámara, desplazamiento desde el origen y orientación, con unidades y convención documentadas. Se registrará explícitamente la pérdida de detección, dejando vacíos los campos de pose cuando corresponda. El formato exacto y la frecuencia se concretarán al implementar y probar.

## Validación prevista

Comparar posiciones conocidas con una regla en los tres ejes, a distintas distancias e inclinaciones; comprobar la orientación con posiciones conocidas y verificar la pérdida y recuperación de detección. Repetir con distintas condiciones de luz y medir el error observado antes de comunicar precisión.
