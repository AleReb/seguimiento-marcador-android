package cl.seguimientomarcador.demo

import android.content.Context
import android.graphics.*
import android.view.View
import org.opencv.core.Point

/** The image and projection share the same sensor-to-screen transform. */
class CubeCameraView(context: Context) : View(context) {
    private var bitmap: Bitmap? = null
    private var points: List<Point> = emptyList()
    private var rotation = 0
    private var mirror = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    fun show(frame: Bitmap, cube: List<Point>, degrees: Int, front: Boolean) {
        val old=bitmap
        bitmap=frame; points=cube; rotation=degrees; mirror=front
        old?.recycle()
        invalidate()
    }
    fun clear() { bitmap?.recycle(); bitmap=null; points=emptyList(); invalidate() }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val frame=bitmap ?: return
        val rotated=rotation%180!=0
        val w=if(rotated) frame.height else frame.width
        val h=if(rotated) frame.width else frame.height
        val scale=minOf(width.toFloat()/w,height.toFloat()/h)
        canvas.save()
        canvas.translate(width/2f,height/2f)
        canvas.scale(if(mirror) -scale else scale,scale)
        canvas.rotate(rotation.toFloat())
        canvas.translate(-frame.width/2f,-frame.height/2f)
        paint.style=Paint.Style.FILL; paint.color=Color.WHITE
        canvas.drawBitmap(frame,0f,0f,paint)
        if(points.size==8) {
            val face=Path()
            face.moveTo(points[4].x.toFloat(),points[4].y.toFloat())
            for(i in 5..7) face.lineTo(points[i].x.toFloat(),points[i].y.toFloat())
            face.close()
            paint.color=Color.argb(80,30,230,160); canvas.drawPath(face,paint)
            paint.color=Color.rgb(30,255,180); paint.strokeWidth=3f/scale
            for(i in 0..3) {
                for((a,b) in listOf(i to (i+1)%4,(i+4) to ((i+1)%4+4),i to (i+4))) {
                    canvas.drawLine(points[a].x.toFloat(),points[a].y.toFloat(),points[b].x.toFloat(),points[b].y.toFloat(),paint)
                }
            }
        }
        canvas.restore()
    }
}
