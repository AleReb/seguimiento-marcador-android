# Cubo Profundidad V2

Versión independiente del demo Unity; `CuboMinimo` sigue disponible sin cambios. Usa el APK Android 0.3 y el mismo protocolo UDP versión 1 en el puerto 5005.

## Abrir

1. En este equipo, buscar **Unity Cubo Profundidad V2** en Super+D. También se puede cerrar otros editores Unity y abrir este directorio mediante **Unity Editor (ARM experimental)**. En Raspberry Pi se requiere FEX, Roslyn ARM y la política de memoria configurada en la instalación.
2. Esperar a que termine la importación y compilación. Pulsar Play y hacer clic en Game para que reciba las teclas.
3. En el celular, elegir el tamaño real del marcador (2 o 5 cm), introducir la IP Wi-Fi actual del computador y pulsar «Enviar a Unity». Ambos dispositivos deben permitir comunicación local.
4. Colocar el marcador en una posición cómoda. La primera pose válida se usa como origen. Acercar y alejar el marcador respecto a la cámara mueve el cubo en Z.

No ejecutar simultáneamente dos demos: ambos necesitan UDP 5005.

## Mini GUI en Game

El panel superior muestra estado UDP, ID del marcador, cambio de profundidad respecto al origen en centímetros, ganancia Z, posición Z de escena y modo de cámara. El aviso recuerda que más ganancia también amplifica el ruido. Usa los botones **− Z**, **+ Z**, **Centrar** y **Cámara**; las teclas equivalentes siguen disponibles. El panel se crea al entrar en Play y usa uGUI con TextMeshPro. Incluye sus recursos esenciales para que no haga falta importarlos mediante un diálogo.

## Cambios de profundidad

- Ganancia lateral **×8** y de profundidad **×12** por defecto. Un cambio medido de 10 cm produce 0,8 unidades en X o Y, y 1,2 unidades en Z.
- Ganancia Z ajustable de ×1 a ×40; teclas `+`/`=` y `−`, o `depthGain` en el Inspector del objeto «Receptor de marcador» durante Play. El valor actual se informa en la consola al cambiarlo. El ajuste durante Play no se guarda al salir.
- Rejilla de referencias cada 2 unidades entre Z −60 y +120. La línea amarilla marca el origen. Son unidades de escena, no distancias físicas de la pantalla. La rejilla no limita el movimiento del cubo.
- Cámara ortográfica oblicua centrada en el origen, con ejes X rojo, Y verde y Z azul. El seguimiento desplaza la cámara en Z cuando el cubo supera 3 unidades de profundidad a cada lado del origen. `C` alterna el seguimiento y la cámara fija.
- Suavizado de posición y orientación, con respuesta aproximada de 83 ms, adicional al filtro del celular. La ganancia amplifica también el ruido; bajarla si el cubo tiembla.
- `R` vuelve a centrar usando la siguiente pose válida. Se admite Input System y el sistema de entrada antiguo.

Sin una pose activa puedes simular: flechas para X/Y, `W`/`S` para Z (8 unidades/s), `Q`/`E` para girar. Tras recibir datos del celular, estos mandan el movimiento; para simular de nuevo detén el envío y pulsa `R`. El cubo se oculta tras 0,5 s sin pose válida.

El tamaño virtual seleccionado en el APK sigue aplicándose como escala relativa: 5 cm corresponden a 0,7 unidades Unity. Es independiente del marcador físico y de la ganancia Z.

## Límites y comprobación manual

Las poses siguen sin calibración óptica y dependen de que el marcador permanezca visible y enfocado. La nueva ganancia no aumenta el rango óptico de detección ni la precisión. La distancia al celular y el desplazamiento desde el origen son conceptos distintos.

Después de compilar, comprobar en un teléfono: recentrado, acercamiento/alejamiento, sensibilidad Z, pérdida y recuperación de señal, cambio de ID, tamaño del cubo, y cámara fija/seguimiento. La compilación por sí sola no valida la interacción visual ni la conexión UDP. El envío es local, sin cifrado ni autenticación.

## Validación del código

Los scripts de profundidad y del panel compilaron con Roslyn ARM usando las referencias Unity del proyecto original. La primera importación completa del proyecto puede tardar bastante en esta Raspberry Pi y muestra errores de API Updater en paquetes heredados. La compilación aislada no demuestra que la GUI, la cámara ni el teléfono funcionen juntos; requieren comprobación visual en Play.

La primera comprobación completa mediante `unity run` alcanzó el límite de 600 segundos durante el procesamiento de bibliotecas (código 14); no completó la importación ni la prueba visual. El registro local está en `~/.local/state/unity-raspberry-pi/profundidad-v2-compilacion.log`. La compilación aislada de ambos scripts con las referencias del proyecto original terminó con código 0.
