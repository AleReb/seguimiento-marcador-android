package cl.seguimientomarcador.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.text.InputType
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import org.opencv.calib3d.Calib3d
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfPoint3f
import org.opencv.core.Point
import org.opencv.core.Point3
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.PI
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.Objdetect
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var status: TextView
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var target: InetAddress? = null
    private val socket = DatagramSocket()
    private var lastSend = 0L
    private lateinit var ipField: EditText
    private lateinit var sendButton: Button
    private var detector: ArucoDetector? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else status.text = "Se necesita permiso de cámara para detectar el marcador."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        previewView = PreviewView(this)
        status = TextView(this).apply {
            text = "Iniciando…"
            textSize = 18f
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER
        }
        ipField = EditText(this).apply {
            hint = "IP del computador (Wi-Fi)"
            setSingleLine(true)
            inputType = InputType.TYPE_CLASS_PHONE
            setText(getPreferences(MODE_PRIVATE).getString("target_ip", "192.168.100.201"))
        }
        sendButton = Button(this).apply {
            text = "Enviar a Unity"
            setOnClickListener {
                if (target != null) {
                    target = null; text = "Enviar a Unity"; ipField.isEnabled = true
                } else {
                    val value = ipField.text.toString().trim()
                    val parts = value.split('.')
                    if (parts.size != 4 || parts.any { it.isEmpty() || it.toIntOrNull() !in 0..255 }) {
                        ipField.error = "Escribe una dirección IPv4 válida"
                    } else {
                        target = InetAddress.getByAddress(parts.map { it.toInt().toByte() }.toByteArray())
                        getPreferences(MODE_PRIVATE).edit().putString("target_ip", value).apply()
                        text = "Detener envío"; ipField.isEnabled = false
                    }
                }
            }
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(previewView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(ipField, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(sendButton)
            })
            addView(Button(this@MainActivity).apply {
                text = "Cambiar cámara"
                setOnClickListener { switchCamera() }
            })
        }
        setContentView(layout)
        if (!OpenCVLoader.initDebug()) {
            status.text = "No se pudo iniciar OpenCV."
            return
        }
        detector = ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50))
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { image ->
                    val gray = Mat(image.height, image.width, CvType.CV_8UC1)
                    val ids = Mat()
                    val corners = ArrayList<Mat>()
                    try {
                        val plane = image.planes[0]
                        val buffer = plane.buffer
                        val row = ByteArray(image.width)
                        for (y in 0 until image.height) {
                            buffer.position(y * plane.rowStride)
                            buffer.get(row, 0, image.width)
                            gray.put(y, 0, row)
                        }
                        detector?.detectMarkers(gray, corners, ids)
                        val message = if (ids.empty()) { sendPose(null); "Buscando marcador ArUco 4x4…" } else {
                            val found = (0 until ids.rows()).map { ids.get(it, 0)[0].toInt() }
                            poseText(found.first(), corners.first(), image.width, image.height)
                        }
                        runOnUiThread { status.text = message }
                    } catch (e: Exception) {
                        runOnUiThread { status.text = "Error al analizar la imagen: ${e.message}" }
                    } finally {
                        corners.forEach { it.release() }
                        ids.release()
                        gray.release()
                        image.close()
                    }
                }
                this.analysis = analysis
                provider.unbindAll()
                provider.bindToLifecycle(this, cameraSelector, preview, analysis)
            } catch (e: Exception) {
                status.text = "No se pudo abrir la cámara: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun switchCamera() {
        val provider = cameraProvider ?: return
        val next = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        try {
            if (!provider.hasCamera(next)) {
                status.text = "Este teléfono no tiene la otra cámara disponible."
                return
            }
            cameraSelector = next
            analysis?.clearAnalyzer()
            status.text = "Cambiando cámara…"
            startCamera()
        } catch (e: Exception) {
            status.text = "No se pudo cambiar de cámara: ${e.message}"
        }
    }

    // Aproximación inicial: el campo visual real de la cámara aún no está calibrado.
    private fun poseText(id: Int, corner: Mat, width: Int, height: Int): String {
        val half = 2.5 // mitad del lado del marcador, en centímetros
        val objectPoints = MatOfPoint3f(
            Point3(-half, half, 0.0), Point3(half, half, 0.0),
            Point3(half, -half, 0.0), Point3(-half, -half, 0.0)
        )
        val points = (0..3).map { i -> corner.get(0, i).let { Point(it[0], it[1]) } }
        val imagePoints = MatOfPoint2f(*points.toTypedArray())
        val camera = Mat.eye(3, 3, CvType.CV_64F)
        val distortion = MatOfDouble(0.0, 0.0, 0.0, 0.0, 0.0)
        val rotation = Mat()
        val translation = Mat()
        val matrix = Mat()
        try {
            // Campo visual horizontal supuesto de 60 grados en la imagen del sensor.
            val focal = width / (2.0 * kotlin.math.tan(PI / 6.0))
            camera.put(0, 0, focal); camera.put(1, 1, focal)
            camera.put(0, 2, width / 2.0); camera.put(1, 2, height / 2.0)
            if (!Calib3d.solvePnP(objectPoints, imagePoints, camera, distortion, rotation, translation, false, Calib3d.SOLVEPNP_IPPE_SQUARE)) {
                sendPose(null)
                return "Marcador $id detectado; no se pudo estimar la posición."
            }
            Calib3d.Rodrigues(rotation, matrix)
            val x = translation.get(0, 0)[0]
            val y = translation.get(1, 0)[0]
            val z = translation.get(2, 0)[0]
            val distance = sqrt(x*x + y*y + z*z)
            val elements = JSONArray()
            for (row in 0..2) for (column in 0..2) elements.put(matrix.get(row, column)[0])
            sendPose(JSONObject().put("version", 1).put("tracked", true).put("id", id)
                .put("x", x).put("y", y).put("z", z).put("rotation", elements))
            val r00 = matrix.get(0, 0)[0]; val r10 = matrix.get(1, 0)[0]
            val r20 = matrix.get(2, 0)[0]; val r21 = matrix.get(2, 1)[0]; val r22 = matrix.get(2, 2)[0]
            val r11 = matrix.get(1, 1)[0]; val r12 = matrix.get(1, 2)[0]
            val sy = sqrt(r00*r00 + r10*r10)
            val rx = if (sy > 1e-6) atan2(r21, r22) else atan2(-r12, r11)
            val ry = atan2(-r20, sy)
            val rz = if (sy > 1e-6) atan2(r10, r00) else 0.0
            return String.format(Locale.US,
                "ID %d · Marcador 5 cm\nX: %.1f  Y: %.1f  Profundidad: %.1f cm\nDistancia: %.1f cm\nÁngulos X: %.0f°  Y: %.0f°  Z: %.0f°\nEstimación sin calibrar · ejes de la cámara",
                id, x, y, z, distance, rx*180/PI, ry*180/PI, rz*180/PI)
        } finally {
            objectPoints.release(); imagePoints.release(); camera.release(); distortion.release()
            rotation.release(); translation.release(); matrix.release()
        }
    }

    private fun sendPose(data: JSONObject?) {
        val destination = target ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastSend < 50) return
        lastSend = now
        try {
            val payload = (data ?: JSONObject().put("version", 1).put("tracked", false)).toString().toByteArray(Charsets.UTF_8)
            socket.send(DatagramPacket(payload, payload.size, destination, 5005))
        } catch (e: Exception) {
            runOnUiThread { status.text = "Error de envío: ${e.message}" }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        target = null
        analysisExecutor.shutdown()
        socket.close()
    }
}
