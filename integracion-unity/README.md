# Demo del marcador en Unity

El archivo `MarcadorDemo.cs` es un componente preparado para copiarse a `Assets/` de un proyecto de Unity 6. Al entrar en Play crea una cámara, una luz, un cubo y un receptor UDP en el puerto 5005. La primera posición y orientación del marcador se toman como origen; la tecla R vuelve a establecerlo. El movimiento se amplifica tres veces para verlo mejor. El cubo desaparece cuando se pierde la señal.

En la aplicación Android, conectada a la misma Wi-Fi, introducir la dirección IPv4 del computador y pulsar «Enviar a Unity». El teléfono envía paquetes JSON UDP al puerto 5005. Para el equipo usado durante el desarrollo la dirección era `192.168.100.201`, pero puede cambiar: comprobar la dirección de la conexión Wi-Fi antes de usarla.

Los datos se envían sin cifrar ni autenticación por la red local. Solo habilita el envío en una red de confianza. El cálculo usa una cámara sin calibrar, por lo que la posición y orientación son estimaciones; los ejes corresponden al sensor de la cámara y podrían no coincidir con la orientación visual del teléfono. El cambio de cámara puede cambiar la referencia: pulsa R para fijar de nuevo el origen.

El editor instalado para este equipo ARM funciona experimentalmente bajo FEX; Unity advierte que la compilación de C# puede fallar. Este archivo aún necesita probarse en una sesión de Unity con licencia activa y en el teléfono.

La versión Android 0.2 permite variar el lado del cubo de 1 a 20 cm y envía `cubeSizeCm` como campo opcional del JSON versión 1. El receptor actualizado lo convierte a una escala relativa: 5 cm mantiene las 0,7 unidades originales; no representa tamaño físico de pantalla. Los receptores anteriores ignoran el campo.

## Nueva versión con mayor profundidad

[CuboProfundidadV2](CuboProfundidadV2/README.md) mantiene el demo anterior y añade ganancia Z ×12 ajustable de ×1 a ×40, rejilla, cámara con seguimiento parcial y suavizado. Usa el mismo APK 0.3. Abrir el proyecto V2 por el lanzador ARM y consultar sus controles antes de entrar en Play.
