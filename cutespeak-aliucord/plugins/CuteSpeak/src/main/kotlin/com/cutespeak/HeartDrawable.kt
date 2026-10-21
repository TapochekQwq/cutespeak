package com.cutespeak

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Сердечко, нарисованное кодом (не зависит от ресурсов Discord). [crossed] — перечёркнутое («выключено»). */
class HeartDrawable(private val color: Int, private val crossed: Boolean) : Drawable() {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; this.color = this@HeartDrawable.color; alpha = if (crossed) 140 else 255 }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.2f; strokeCap = Paint.Cap.ROUND; this.color = 0xFFED4245.toInt() }

    private val heart = Path().apply {
        moveTo(12f, 21.35f); lineTo(10.55f, 20.03f)
        cubicTo(5.4f, 15.36f, 2f, 12.28f, 2f, 8.5f)
        cubicTo(2f, 5.42f, 4.42f, 3f, 7.5f, 3f)
        cubicTo(9.24f, 3f, 10.91f, 3.81f, 12f, 5.09f)
        cubicTo(13.09f, 3.81f, 14.76f, 3f, 16.5f, 3f)
        cubicTo(19.58f, 3f, 22f, 5.42f, 22f, 8.5f)
        cubicTo(22f, 12.28f, 18.6f, 15.36f, 13.45f, 20.04f)
        lineTo(12f, 21.35f); close()
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        canvas.scale(b.width() / 24f, b.height() / 24f)
        canvas.drawPath(heart, fill)
        if (crossed) canvas.drawLine(3f, 3f, 21f, 21f, stroke)
        canvas.restore()
    }

    override fun getIntrinsicWidth() = 64
    override fun getIntrinsicHeight() = 64
    override fun setAlpha(alpha: Int) { fill.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { fill.colorFilter = colorFilter }
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = PixelFormat.TRANSLUCENT

    companion object {
        const val PINK = 0xFFFF7EB6.toInt()
    }
}
