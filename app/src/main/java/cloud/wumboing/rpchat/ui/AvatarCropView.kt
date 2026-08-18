package cloud.wumboing.rpchat.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

/**
 * Area penyesuaian foto profil: bisa digeser (pan) dan dicubit untuk zoom.
 * Area lingkaran di tengah adalah bagian yang akan dipakai sebagai avatar.
 */
class AvatarCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var bitmap: Bitmap? = null
    private var scale = 1f
    private var minScale = 1f
    private var maxScale = 1f
    private var offsetX = 0f
    private var offsetY = 0f

    private var cropRadius = 0f
    private var cropCx = 0f
    private var cropCy = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isScaling = false

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint().apply { color = Color.parseColor("#B0000000") }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isScaling = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (scale * detector.scaleFactor).coerceIn(minScale, maxScale)
            val fx = detector.focusX
            val fy = detector.focusY
            offsetX = fx - (fx - offsetX) * (newScale / scale)
            offsetY = fy - (fy - offsetY) * (newScale / scale)
            scale = newScale
            clampOffsets()
            invalidate()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isScaling = false
        }
    })

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cropCx = w / 2f
        cropCy = h / 2f
        cropRadius = minOf(w, h) * 0.42f
        bitmap?.let { recomputeInitial(it) }
    }

    fun setImageBitmap(bmp: Bitmap) {
        bitmap = bmp
        if (width > 0 && height > 0) {
            recomputeInitial(bmp)
        }
        invalidate()
    }

    private fun recomputeInitial(bmp: Bitmap) {
        val cropSize = cropRadius * 2f
        minScale = maxOf(cropSize / bmp.width, cropSize / bmp.height)
        maxScale = minScale * 4f
        scale = minScale
        offsetX = cropCx - bmp.width * scale / 2f
        offsetY = cropCy - bmp.height * scale / 2f
        clampOffsets()
    }

    private fun clampOffsets() {
        val bmp = bitmap ?: return
        val drawnW = bmp.width * scale
        val drawnH = bmp.height * scale
        val left = cropCx - cropRadius
        val right = cropCx + cropRadius
        val top = cropCy - cropRadius
        val bottom = cropCy + cropRadius

        if (offsetX > left) offsetX = left
        if (offsetX + drawnW < right) offsetX = right - drawnW
        if (offsetY > top) offsetY = top
        if (offsetY + drawnH < bottom) offsetY = bottom - drawnH
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isScaling && event.pointerCount == 1) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    offsetX += dx
                    offsetY += dy
                    clampOffsets()
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = bitmap ?: return

        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bmp, 0f, 0f, bitmapPaint)
        canvas.restore()

        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawCircle(cropCx, cropCy, cropRadius, clearPaint)
        canvas.restoreToCount(layer)

        canvas.drawCircle(cropCx, cropCy, cropRadius, borderPaint)
    }

    /** Hasil crop persegi (nanti ditampilkan bulat lewat clipToCircle). */
    fun getCroppedBitmap(outputSize: Int = 512): Bitmap? {
        val bmp = bitmap ?: return null
        val left = cropCx - cropRadius
        val top = cropCy - cropRadius
        val right = cropCx + cropRadius
        val bottom = cropCy + cropRadius

        val srcLeft = ((left - offsetX) / scale)
        val srcTop = ((top - offsetY) / scale)
        val srcRight = ((right - offsetX) / scale)
        val srcBottom = ((bottom - offsetY) / scale)

        val srcRect = Rect(
            srcLeft.toInt().coerceIn(0, bmp.width),
            srcTop.toInt().coerceIn(0, bmp.height),
            srcRight.toInt().coerceIn(0, bmp.width),
            srcBottom.toInt().coerceIn(0, bmp.height)
        )
        if (srcRect.width() <= 0 || srcRect.height() <= 0) return null

        val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val dstRect = RectF(0f, 0f, outputSize.toFloat(), outputSize.toFloat())
        canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
        return output
    }
}
