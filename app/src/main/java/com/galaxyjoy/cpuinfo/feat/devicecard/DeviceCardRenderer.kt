package com.galaxyjoy.cpuinfo.feat.devicecard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.galaxyjoy.cpuinfo.R

/**
 * Draws the U14 shareable "device ID card" — a fixed-size portrait [Bitmap], not a screenshot of
 * any real screen. Plain `Canvas`/`Paint` (not Compose `captureToImage()`/an off-screen
 * `ComposeView`) — simpler and more predictable for a fixed layout than measuring/laying out a
 * Compose tree with no attached window. Can't be unit-tested (android.graphics.* is stubbed under
 * this project's JVM `unitTests.isReturnDefaultValues = true` setup, same reason
 * [com.galaxyjoy.cpuinfo.feat.shieldwidget.ShieldScoreWidgetProvider] avoids `Color.parseColor`) —
 * verified instead on a real device in [DeviceCardRendererTest] (androidTest).
 */
object DeviceCardRenderer {

    const val CARD_WIDTH_PX = 1080
    const val CARD_HEIGHT_PX = 1350
    private const val MARGIN_PX = 60f
    private const val PANEL_CORNER_RADIUS_PX = 32f

    fun render(context: Context, data: DeviceCardData): Bitmap {
        val bitmap = createBitmap(CARD_WIDTH_PX, CARD_HEIGHT_PX)
        val canvas = Canvas(bitmap)

        val primaryColor = ContextCompat.getColor(context, R.color.primary)
        val surfaceColor = ContextCompat.getColor(context, R.color.surface)
        val onSurfaceColor = ContextCompat.getColor(context, R.color.onSurface)
        val onSurfaceVariant = androidx.core.graphics.ColorUtils.setAlphaComponent(onSurfaceColor, 160)

        canvas.drawColor(primaryColor)

        val panelRect = RectF(MARGIN_PX, MARGIN_PX, CARD_WIDTH_PX - MARGIN_PX, CARD_HEIGHT_PX - MARGIN_PX)
        val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = surfaceColor }
        canvas.drawRoundRect(panelRect, PANEL_CORNER_RADIUS_PX, PANEL_CORNER_RADIUS_PX, panelPaint)

        val contentLeft = panelRect.left + 48f
        val contentRight = panelRect.right - 48f
        var y = panelRect.top + 90f

        // App label, top-left.
        val labelPaint = textPaint(size = 28f, color = onSurfaceVariant, bold = false)
        canvas.drawText(context.getString(R.string.device_card_app_label), contentLeft, y, labelPaint)

        // Shield Score badge, top-right — same 3-band thresholds as
        // ShieldScoreWidgetProvider/ShieldScoreBottomSheet's scoreColor().
        data.shieldScore?.let { score ->
            drawScoreBadge(canvas, score, centerX = contentRight - 50f, centerY = y - 15f)
        }

        y += 90f
        val modelPaint = textPaint(size = 56f, color = onSurfaceColor, bold = true)
        y = drawWrappedText(canvas, data.deviceModel, contentLeft, y, contentRight - contentLeft, modelPaint)

        y += 20f
        val chipPaint = textPaint(size = 34f, color = onSurfaceVariant, bold = false)
        y = drawWrappedText(canvas, data.chipName, contentLeft, y, contentRight - contentLeft, chipPaint)

        y += 50f
        val dividerPaint = Paint().apply { color = onSurfaceVariant; strokeWidth = 2f }
        canvas.drawLine(contentLeft, y, contentRight, y, dividerPaint)
        y += 70f

        val rowLabelPaint = textPaint(size = 32f, color = onSurfaceVariant, bold = false)
        val rowValuePaint = textPaint(size = 32f, color = onSurfaceColor, bold = true).apply { textAlign = Paint.Align.RIGHT }

        val rows = listOf(
            context.getString(R.string.device_card_row_cores) to data.coreCount.toString(),
            context.getString(R.string.device_card_row_ram) to DeviceCardData.formatGb(data.ramTotalBytes),
            context.getString(R.string.device_card_row_storage) to DeviceCardData.formatGb(data.storageTotalBytes),
            context.getString(R.string.device_card_row_display) to
                "${data.screenResolution} · ${data.refreshRateHz.takeIf { it > 0 }?.let { "$it Hz" } ?: "—"}",
            context.getString(R.string.device_card_row_android) to data.androidVersion,
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

    private fun drawScoreBadge(canvas: Canvas, score: Int, centerX: Float, centerY: Float) {
        val radius = 60f
        val color = when {
            score >= 80 -> 0xFF4CAF50.toInt()
            score >= 50 -> 0xFFFFA726.toInt()
            else -> 0xFFE53935.toInt()
        }
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawCircle(centerX, centerY, radius, badgePaint)

        val scorePaint = textPaint(size = 40f, color = 0xFFFFFFFF.toInt(), bold = true).apply {
            textAlign = Paint.Align.CENTER
        }
        // Vertically center text on the circle: baseline offset by half the font's ascent+descent.
        val textY = centerY - (scorePaint.ascent() + scorePaint.descent()) / 2f
        canvas.drawText(score.toString(), centerX, textY, scorePaint)
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
}
