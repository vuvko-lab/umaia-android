package app.umaia.android.ui.screens.steps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import app.umaia.android.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object StepShareUtils {

    /**
     * Loads `R.drawable.logo_umaia` scaled to [maxSide] for compositing into
     * share images. Pulled out so both image factories can reuse it.
     */
    private fun loadLogo(context: Context, maxSide: Int): Bitmap? = runCatching {
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.logo_umaia) ?: return null
        val ratio = raw.height.toFloat() / raw.width.toFloat()
        val w = maxSide
        val h = (maxSide * ratio).toInt()
        Bitmap.createScaledBitmap(raw, w, h, true)
    }.getOrNull()

    /** Composites the Umaia logo above the brand text on a 1080×1920 share canvas. */
    private fun drawLogoFooter(canvas: Canvas, context: Context, width: Int) {
        val logo = loadLogo(context, maxSide = 240) ?: return
        val cx = width / 2f - logo.width / 2f
        val cy = 1700f - logo.height / 2f  // ~120 px above the brand-text baseline at y=1850
        canvas.drawBitmap(logo, cx, cy, null)
    }

    fun createTodayStepsImage(
        context: Context,
        steps: Int,
        nur: Int,
        stepGoal: Int = 10_000
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient (dark)
        val bgPaint = Paint().apply {
            color = 0xFF1a1a2e.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val goldColor = 0xFFd4af37.toInt()
        val emberColor = 0xFFe67e22.toInt()

        // Header
        val headerPaint = Paint().apply {
            color = emberColor
            textSize = 60f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("👣", width / 2f, 150f, headerPaint)

        // Steps count
        val stepsPaint = Paint().apply {
            color = goldColor
            textSize = 120f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        canvas.drawText(steps.toString(), width / 2f, 400f, stepsPaint)

        // Label
        val labelPaint = Paint().apply {
            color = AndroidColor.parseColor("#888888")
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("steps today", width / 2f, 480f, labelPaint)

        // Progress ring background
        val ringRadius = 150f
        val ringPaint = Paint().apply {
            color = AndroidColor.parseColor("#333333")
            style = Paint.Style.STROKE
            strokeWidth = 20f
            isAntiAlias = true
        }
        canvas.drawCircle(width / 2f, 700f, ringRadius, ringPaint)

        // Progress ring fill
        val progress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f)
        val progressPaint = Paint().apply {
            color = emberColor
            style = Paint.Style.STROKE
            strokeWidth = 20f
            isAntiAlias = true
        }
        canvas.drawArc(
            width / 2f - ringRadius,
            700f - ringRadius,
            width / 2f + ringRadius,
            700f + ringRadius,
            -90f,
            360f * progress,
            false,
            progressPaint
        )

        // Nur earned
        val nurPaint = Paint().apply {
            color = goldColor
            textSize = 50f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("⚡ +$nur Nur", width / 2f, 1100f, nurPaint)

        // Date
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val datePaint = Paint().apply {
            color = AndroidColor.parseColor("#888888")
            textSize = 35f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(dateFormat.format(Date()), width / 2f, 1200f, datePaint)

        // Footer branding (logo + wordmark) — so a re-shared image carries
        // Umaia's brand even on platforms that strip alt text.
        drawLogoFooter(canvas, context, width)
        val brandPaint = Paint().apply {
            color = AndroidColor.parseColor("#666666")
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Umaia", width / 2f, 1850f, brandPaint)

        return bitmap
    }

    fun createCalendarImage(
        context: Context,
        stepHistory: Map<String, Int>,
        currentMonth: Calendar = Calendar.getInstance()
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = 0xFF1a1a2e.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val goldColor = 0xFFd4af37.toInt()
        val emberColor = 0xFFe67e22.toInt()

        // Header
        val headerPaint = Paint().apply {
            color = emberColor
            textSize = 60f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📅", width / 2f, 150f, headerPaint)

        // Title
        val titlePaint = Paint().apply {
            color = goldColor
            textSize = 50f
            textAlign = Paint.Align.CENTER
        }
        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(currentMonth.time)
        canvas.drawText(monthName, width / 2f, 280f, titlePaint)

        // Calendar grid
        val cellSize = 120f
        val startX = (width - cellSize * 7) / 2
        val startY = 400f
        val dayLabels = arrayOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

        // Day labels
        val dayLabelPaint = Paint().apply {
            color = AndroidColor.parseColor("#888888")
            textSize = 25f
            textAlign = Paint.Align.CENTER
        }
        for (i in dayLabels.indices) {
            canvas.drawText(
                dayLabels[i],
                startX + cellSize / 2 + i * cellSize,
                startY - 20,
                dayLabelPaint
            )
        }

        // Days
        val cal = Calendar.getInstance().apply { set(currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH), 1) }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1

        val cellPaint = Paint().apply {
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            textSize = 30f
            textAlign = Paint.Align.CENTER
            color = AndroidColor.WHITE
        }

        for (day in 1..maxDays) {
            val column = (firstDayOfWeek + day - 1) % 7
            val row = (firstDayOfWeek + day - 1) / 7
            val x = startX + cellSize / 2 + column * cellSize
            val y = startY + cellSize / 2 + row * cellSize

            val dateStr = String.format("%04d-%02d-%02d", currentMonth.get(Calendar.YEAR), currentMonth.get(Calendar.MONTH) + 1, day)
            val stepsForDay = stepHistory[dateStr] ?: 0
            val stepGoal = 10_000
            val progressPercent = (stepsForDay.toFloat() / stepGoal).coerceIn(0f, 1f)

            // Cell background based on progress
            cellPaint.color = when {
                stepsForDay >= stepGoal -> emberColor
                stepsForDay >= stepGoal * 0.5f -> 0xFF8B6F47.toInt()
                stepsForDay > 0 -> 0xFF5a4a3a.toInt()
                else -> 0xFF2a2a3a.toInt()
            }
            canvas.drawRect(x - cellSize / 2 + 5, y - cellSize / 2 + 5, x + cellSize / 2 - 5, y + cellSize / 2 - 5, cellPaint)

            // Day number
            textPaint.color = AndroidColor.WHITE
            canvas.drawText(day.toString(), x, y + 15, textPaint)
        }

        // Footer (logo + wordmark)
        drawLogoFooter(canvas, context, width)
        val footerPaint = Paint().apply {
            color = AndroidColor.parseColor("#666666")
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Umaia Step Calendar", width / 2f, 1900f, footerPaint)

        return bitmap
    }

    fun saveBitmapAndShare(context: Context, bitmap: Bitmap, fileName: String): Boolean {
        return try {
            val cacheDir = File(context.cacheDir, "share")
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Step Image"))
            true
        } catch (e: Exception) {
            android.util.Log.e("StepShare", "Failed to share image", e)
            false
        }
    }
}
