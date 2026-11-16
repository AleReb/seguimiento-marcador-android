package cl.seguimientomarcador.demo

import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.objdetect.*
import kotlin.math.*

/** Corners are filtered in sensor coordinates; camera calibration is still approximate. */
class MarkerTracker {
    private val detector = ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_50),
        DetectorParameters().apply { set_cornerRefinementMethod(Objdetect.CORNER_REFINE_SUBPIX) })
    private var selected = -1
    private var previous: Array<Point>? = null
    private var previousTime = 0L
    data class Result(val text: String, val packet: JSONObject?, val cube: List<Point> = emptyList())

    fun reset() { selected = -1; previous = null; previousTime = 0L }

    fun process(gray: Mat, cubeSizeCm: Float, markerSizeCm: Float): Result {
        val ids = Mat()
        val corners = ArrayList<Mat>()
        try {
            detector.detectMarkers(gray, corners, ids)
            if (ids.empty()) {
                if (SystemClock.elapsedRealtime() - previousTime > 250) reset()
                return Result("Buscando marcador ArUco 4x4…", null)
            }
            val candidates = corners.indices
            val same = candidates.firstOrNull { ids.get(it, 0)[0].toInt() == selected }
            val index = same ?: candidates.maxByOrNull { i ->
                val p = (0..3).map { corners[i].get(0, it) }
                abs(p.indices.sumOf { j -> p[j][0]*p[(j+1)%4][1] - p[(j+1)%4][0]*p[j][1] })
            }!!
            val id = ids.get(index, 0)[0].toInt()
            val now = SystemClock.elapsedRealtime()
            val raw = Array(4) { corners[index].get(0, it).let { v -> Point(v[0], v[1]) } }
            val old = previous
            val alpha = (1.0 - exp(-(now - previousTime).coerceAtLeast(1) / 65.0)).coerceIn(0.1, 1.0)
            val points = if (old == null || id != selected || now - previousTime > 250) raw else
                Array(4) { Point(old[it].x + alpha*(raw[it].x-old[it].x), old[it].y + alpha*(raw[it].y-old[it].y)) }
            return estimate(id, points, gray.cols(), gray.rows(), cubeSizeCm, markerSizeCm).also {
                if (it.packet != null) { selected = id; previous = points; previousTime = now }
                else reset()
            }
        } finally { ids.release(); corners.forEach { it.release() } }
    }

    private fun estimate(id: Int, points: Array<Point>, width: Int, height: Int, side: Float, markerSide: Float): Result {
        val half = markerSide.toDouble()/2
        val objectPoints = MatOfPoint3f(Point3(-half,half,0.0), Point3(half,half,0.0), Point3(half,-half,0.0), Point3(-half,-half,0.0))
        val imagePoints = MatOfPoint2f(*points)
        val camera = Mat.eye(3,3,CvType.CV_64F)
        val distortion = MatOfDouble(0.0,0.0,0.0,0.0,0.0)
        val rotation = Mat(); val translation = Mat(); val matrix = Mat()
        val projected = MatOfPoint2f(); val vertices = MatOfPoint3f(); val cube = MatOfPoint2f()
        try {
            val focal = width / (2.0*tan(PI/6.0))
            camera.put(0,0,focal); camera.put(1,1,focal)
            camera.put(0,2,width/2.0); camera.put(1,2,height/2.0)
            if (!Calib3d.solvePnP(objectPoints,imagePoints,camera,distortion,rotation,translation,false,Calib3d.SOLVEPNP_IPPE_SQUARE))
                return Result("ID $id · pose no disponible", null)
            Calib3d.projectPoints(objectPoints,rotation,translation,camera,distortion,projected)
            val reprojection = projected.toArray()
            val error = sqrt(points.indices.sumOf { i -> (points[i].x-reprojection[i].x).pow(2)+(points[i].y-reprojection[i].y).pow(2) }/4.0)
            val x=translation.get(0,0)[0]; val y=translation.get(1,0)[0]; val z=translation.get(2,0)[0]
            if (!listOf(x,y,z,error).all { it.isFinite() } || z <= 0 || error > 4.0)
                return Result("ID $id · detección inestable; mejora luz o enfoque", null)
            Calib3d.Rodrigues(rotation,matrix)
            val elements=JSONArray()
            for (r in 0..2) for (c in 0..2) elements.put(matrix.get(r,c)[0])
            val h=side.toDouble()/2
            // Positive marker Z points out of the printed marker towards the viewer.
            val v=arrayOf(Point3(-h,h,0.0),Point3(h,h,0.0),Point3(h,-h,0.0),Point3(-h,-h,0.0),
                Point3(-h,h,side.toDouble()),Point3(h,h,side.toDouble()),Point3(h,-h,side.toDouble()),Point3(-h,-h,side.toDouble()))
            vertices.fromArray(*v)
            val inFront=v.all { matrix.get(2,0)[0]*it.x+matrix.get(2,1)[0]*it.y+matrix.get(2,2)[0]*it.z+z > 0.1 }
            Calib3d.projectPoints(vertices,rotation,translation,camera,distortion,cube)
            val display=if(inFront) cube.toList().filter { it.x.isFinite() && it.y.isFinite() && abs(it.x)<100000 && abs(it.y)<100000 } else emptyList()
            val packet=JSONObject().put("version",1).put("tracked",true).put("id",id)
                .put("x",x).put("y",y).put("z",z).put("rotation",elements).put("cubeSizeCm",side.toDouble())
            val text=String.format(java.util.Locale.US,"ID %d · Marcador %.0f cm · Cubo %.1f cm\nX %.1f · Y %.1f · Z %.1f cm · Distancia %.1f cm\nEstimación sin calibrar · Error de ajuste %.1f px",id,markerSide,side,x,y,z,sqrt(x*x+y*y+z*z),error)
            return Result(text,packet,if(display.size==8) display else emptyList())
        } finally {
            listOf(objectPoints,imagePoints,camera,distortion,rotation,translation,matrix,projected,vertices,cube).forEach { it.release() }
        }
    }
}
