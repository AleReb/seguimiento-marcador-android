# Versión 0.3 — seguimiento, proyección y tamaños

APK: [demo-marcador-v0.3.apk](../apk/demo-marcador-v0.3.apk). Se conserva el APK anterior `demo-marcador-udp.apk`.

- Selección persistente de marcador impreso de 2 o 5 cm; el valor inicial es 5 cm por compatibilidad. Seleccionar el lado exterior negro real, sin incluir el margen blanco.
- Refinamiento subpíxel y suavizado de las esquinas; conservación del ID visible y rechazo de poses con ajuste inestable.
- Cubo proyectado sobre la imagen de cámara; tamaño virtual ajustable entre 1 y 20 cm.
- Campo opcional `cubeSizeCm` en UDP; los receptores Unity actualizados lo aplican como escala relativa. Los antiguos ignoran ese campo.
- PDFs A4 y Carta para impresión, marcadores de 2 cm y plantillas de cubos de papel de 2 y 5 cm.

El APK tiene `versionCode=3`, `versionName=0.3` y usa la misma identidad de aplicación. Es una compilación debug; se puede instalar como actualización de la anterior si comparte firma. La app anterior no se mantiene instalada en paralelo. Para volver a una versión con código inferior Android puede requerir desinstalar primero, lo que elimina las preferencias.

La compilación se verifica con Gradle `assembleDebug`; las cámaras, la proyección, la estabilidad y el envío de tamaño a Unity requieren prueba en el teléfono. No se ha implementado aún el asistente de calibración; los parámetros ópticos siguen siendo aproximados.
