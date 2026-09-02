package com.galaxyjoy.cpuinfo.feat.benchresultcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import com.galaxyjoy.cpuinfo.R
import com.galaxyjoy.cpuinfo.feat.allbench.VMAllBench
import java.util.Locale

/**
 * U23 — shareable "benchmark results" card, sibling to
 * [com.galaxyjoy.cpuinfo.feat.devicecard.DeviceCardRenderer] (U14) rather than a shared base:
 * that class's own KDoc already documents the same fixed-offset Canvas/`StaticLayout` drawing
 * style as deliberately simple/copy-friendly, and this is the only 2nd variant so far — a shared
 * base would only pay for itself with a 3rd. Same reason it can't be unit-tested (android.graphics
 * stubbed under this project's JVM `unitTests.isReturnDefaultValues = true`) — verified on a real
 * device in [BenchResultCardRendererTest].
 */
object BenchResultCardRenderer {

    const val CARD_WIDTH_PX = 1080
    const val CARD_HEIGHT_PX = 1350
    private const val MARGIN_PX = 60f
    private const val PANEL_CORNER_RADIUS_PX = 32f

    fun render(context: Context, results: VMAllBench.Results): Bitmap {
        val bitmap = createBitmap(CARD_WIDTH_PX, CARD_HEIGHT_PX)
        val canvas = Canvas(bitmap)

        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        val surfaceColor = ContextCompat.getColor(context, R.color.surface)
        val onSurfaceColor = ContextCompat.getColor(context, R.color.onSurface)
        val onSurfaceVariant = ColorUtils.setAlphaComponent(onSurfaceColor, 160)

        canvas.drawColor(primaryColor)

        val panelRect = RectF(MARGIN_PX, MARGIN_PX, CARD_WIDTH_PX - MARGIN_PX, CARD_HEIGHT_PX - MARGIN_PX)
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceColor }
        canvas.drawRoundRect(panelRect, PANEL_CORNER_RADIUS_PX, PANEL_CORNER_RADIUS_PX, panelPaint)

        val contentLeft = panelRect.left + 48f
        val contentRight = panelRect.right - 48f
        var y = panelRect.top + 90f

        val labelPaint = textPaint(size = 28f, color = onSurfaceVariant, bold = false)
        canvas.drawText(context.getString(R.string.device_card_app_label), contentLeft, y, labelPaint)

        y += 90f
        val titlePaint = textPaint(size = 48f, color = onSurfaceColor, bold = true)
        y = drawWrappedText(canvas, context.getString(R.string.bench_result_card_title), contentLeft, y, contentRight - contentLeft, titlePaint)

        y += 16f
        val modelPaint = textPaint(size = 30f, color = onSurfaceVariant, bold = false)
        y = drawWrappedText(canvas, Build.MODEL, contentLeft, y, contentRight - contentLeft, modelPaint)

        y += 50f
        val dividerPaint = Paint().apply { color = onSurfaceVariant; strokeWidth = 2f }
        canvas.drawLine(contentLeft, y, contentRight, y, dividerPaint)
        y += 70f

        val rowLabelPaint = textPaint(size = 32f, color = onSurfaceVariant, bold = false)
        val rowValuePaint = textPaint(size = 32f, color = onSurfaceColor, bold = true).apply { textAlign = Paint.Align.RIGHT }

        val rows = listOf(
            context.getString(R.string.all_bench_row_throttle) to "${results.throttle.sustainedFreqMhz} MHz",
            context.getString(R.string.all_bench_row_storage) to
                "${formatDecimal(results.storage.seqWriteMbPerSec)}/${formatDecimal(results.storage.seqReadMbPerSec)} MB/s",
            context.getString(R.string.all_bench_row_ram) to
                "${formatDecimal(results.ram.writeMbPerSec)}/${formatDecimal(results.ram.readMbPerSec)} MB/s",
            context.getString(R.string.all_bench_row_gpu) to "${formatDecimal(results.gpu.avgFps)} FPS",
        )
        rows.forEach { (label, value) ->
            canvas.drawText(label, contentLeft, y, rowLabelPaint)
            canvas.drawText(value, contentRight, y, rowValuePaint)
            y += 64f
        }

        val footerPaint = textPaint(size = 24f, color = onSurfaceVariant, bold = false).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(
            context.getString(R.string.device_card_footer),
            (contentLeft + contentRight) / 2f,
            panelRect.bottom - 40f,
            footerPaint,
        )

        return bitmap
    }

    /** @return the y position just below the last drawn line, ready for the next block. */
    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, startY: Float, maxWidth: Float, paint: TextPaint): Float {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth.toInt())
            .build()
        canvas.save()
        canvas.translate(x, startY - paint.fontMetrics.ascent)
        layout.draw(canvas)
        canvas.restore()
        return startY - paint.fontMetrics.ascent + layout.height + paint.fontMetrics.descent
    }

    private fun textPaint(size: Float, color: Int, bold: Boolean): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        isFakeBoldText = bold
    }

    /** Always renders with a "." decimal point, regardless of the device's default locale. */
    private fun formatDecimal(value: Double): String = "%.1f".format(Locale.US, value)
}
