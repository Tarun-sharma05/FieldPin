package com.tarun.fieldpin.data.photo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhotoStamp(
    val siteName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double,
    val takenAt: Long
)

fun formatPhotoStamp(stamp: PhotoStamp, locale: Locale = Locale.getDefault()): List<String> = listOf(
    stamp.siteName,
    "${"%.6f".format(locale, stamp.latitude)}, ${"%.6f".format(locale, stamp.longitude)}",
    "Accuracy ±${"%.1f".format(locale, stamp.accuracyM)} m",
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale).format(Date(stamp.takenAt))
)

class PhotoStamper {
    fun stamp(file: File, stamp: PhotoStamp) {
        val decoded = BitmapFactory.decodeFile(file.absolutePath)
            ?: error("The captured photo could not be decoded.")
        val orientation = ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> preScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { preScale(-1f, 1f); postRotate(90f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { preScale(-1f, 1f); postRotate(270f) }
            }
        }
        val oriented = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (oriented !== decoded) decoded.recycle()
        val mutable = oriented.copy(Bitmap.Config.ARGB_8888, true)
            ?: error("The captured photo could not be prepared for stamping.")
        if (oriented !== mutable) oriented.recycle()

        val canvas = Canvas(mutable)
        val lines = formatPhotoStamp(stamp)
        val fontSize = (mutable.width * 0.035f).coerceAtLeast(22f)
        val padding = fontSize * 0.65f
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = fontSize
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val rowHeight = fontSize * 1.35f
        val boxHeight = padding * 2 + rowHeight * lines.size
        val box = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB8000000.toInt() }
        canvas.drawRoundRect(
            RectF(padding / 2, mutable.height - boxHeight - padding / 2, mutable.width - padding / 2, mutable.height - padding / 2),
            padding / 2,
            padding / 2,
            box
        )
        lines.forEachIndexed { index, line ->
            canvas.drawText(line, padding, mutable.height - boxHeight + padding + rowHeight * (index + 1) - fontSize * 0.2f, textPaint)
        }

        val temp = File(file.parentFile, "${file.nameWithoutExtension}_stamped.jpg")
        FileOutputStream(temp).use { output ->
            check(mutable.compress(Bitmap.CompressFormat.JPEG, 94, output)) { "Could not save the stamped photo." }
        }
        mutable.recycle()
        check(temp.renameTo(file) || temp.copyTo(file, overwrite = true).exists()) {
            "Could not replace the captured photo with its stamped copy."
        }
        temp.delete()
    }
}
