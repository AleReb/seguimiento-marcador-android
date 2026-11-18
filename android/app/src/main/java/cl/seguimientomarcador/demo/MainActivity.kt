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
import org.json.JSONObject
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var cameraView: CubeCameraView
    private lateinit var status: TextView
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    @Volatile private var target: InetAddress? = null
    private val socket = DatagramSocket()
    private var lastSend = 0L
    private lateinit var ipField: EditText
    private lateinit var sendButton: Button
    private var tracker: MarkerTracker? = null
    @Volatile private var cubeSizeCm = 5f
    @Volatile private var markerSizeCm = 5f
    private val framePending = java.util.concurrent.atomic.AtomicBoolean(false)
    @Volatile private var cameraGeneration = 0
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null
    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else status.text = "Se necesita permiso de cámara para detectar el marcador."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraView = CubeCameraView(this)
        cubeSizeCm = getPreferences(MODE_PRIVATE).getFloat("cube_size", 5f).coerceIn(1f,20f)
        markerSizeCm = getPreferences(MODE_PRIVATE).getFloat("marker_size", 5f).let { if (it == 2f) 2f else 5f }
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
            setText(getPreferences(MODE_PRIVATE).getString("target_ip", ""))
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
            addView(cameraView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(TextView(this@MainActivity).apply { text = "Lado del marcador impreso (no del cubo):" })
            addView(android.widget.RadioGroup(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                val small = android.widget.RadioButton(this@MainActivity).apply { id = android.view.View.generateViewId(); text = "2 cm" }
                val large = android.widget.RadioButton(this@MainActivity).apply { id = android.view.View.generateViewId(); text = "5 cm" }
                addView(small); addView(large)
                check(if (markerSizeCm == 2f) small.id else large.id)
                setOnCheckedChangeListener { _, checked ->
                    markerSizeCm = if (checked == small.id) 2f else 5f
                    getPreferences(MODE_PRIVATE).edit().putFloat("marker_size", markerSizeCm).apply()
                    analysisExecutor.execute { tracker?.reset() }
                }
            })
            val sizeLabel = TextView(this@MainActivity).apply { text = "Tamaño del cubo: %.1f cm".format(cubeSizeCm) }
            addView(sizeLabel)
            addView(android.widget.SeekBar(this@MainActivity).apply {
                max = 190
                progress = ((cubeSizeCm - 1f) * 10).toInt()
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: android.widget.SeekBar?, value: Int, user: Boolean) {
                        cubeSizeCm = 1f + value / 10f
                        sizeLabel.text = "Tamaño del cubo: %.1f cm".format(cubeSizeCm)
                    }
                    override fun onStartTrackingTouch(bar: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(bar: android.widget.SeekBar?) {
                        getPreferences(MODE_PRIVATE).edit().putFloat("cube_size", cubeSizeCm).apply()
                    }
                })
            })
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
        tracker = MarkerTracker()
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
                val generation = ++cameraGeneration
                analysisExecutor.execute { tracker?.reset() }
                cameraView.clear()
                val front = cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(640, 480))
                    .build()
                analysis.setAnalyzer(analysisExecutor) { image ->
                    if (generation != cameraGeneration || !framePending.compareAndSet(false, true)) {
                        image.close()
                        return@setAnalyzer
                    }
                    var delivered = false
                    val gray = Mat(image.height, image.width, CvType.CV_8UC1)
                    try {
                        val plane = image.planes[0]
                        val buffer = plane.buffer.duplicate()
                        val row = ByteArray(image.width)
                        for (y in 0 until image.height) {
                            buffer.position(y * plane.rowStride)
                            buffer.get(row, 0, image.width)
                            gray.put(y, 0, row)
                        }
                        val result = tracker?.process(gray, cubeSizeCm, markerSizeCm)
                        sendPose(result?.packet)
                        val bitmap = image.toBitmap()
                        val degrees = image.imageInfo.rotationDegrees
                        runOnUiThread {
                            try {
                                if (!isDestroyed && generation == cameraGeneration) {
                                    cameraView.show(bitmap, result?.cube ?: emptyList(), degrees, front)
                                    status.text = result?.text ?: "Iniciando…"
                                } else bitmap.recycle()
                            } finally { framePending.set(false) }
                        }
                        delivered = true
                    } catch (e: Exception) {
                        runOnUiThread { status.text = "Error al analizar la imagen: ${e.message}" }
                    } finally {
                        gray.release()
                        if (!delivered) framePending.set(false)
                        image.close()
                    }
                }
                this.analysis = analysis
                provider.unbindAll()
                provider.bindToLifecycle(this, cameraSelector, analysis)
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
        cameraGeneration++
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        cameraView.clear()
        analysisExecutor.shutdown()
        socket.close()
    }
}
