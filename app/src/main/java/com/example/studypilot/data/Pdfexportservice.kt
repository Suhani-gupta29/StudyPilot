package com.example.studypilot.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExportService(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595   // A4 width in points (72dpi)
        private const val PAGE_HEIGHT = 842  // A4 height in points (72dpi)
        private const val MARGIN = 60f
        private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    }

    /**
     * Exports a list of notes to a PDF file saved in the Downloads folder.
     * Returns a Result containing the Uri of the saved file, or an error.
     */
    suspend fun exportNotesToPdf(
        notes: List<Note>,
        subjectName: String,
        mode: String
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()
            var pageNumber = 0
            var currentY = 0f
            var currentPage: PdfDocument.Page? = null
            var canvas: Canvas? = null

            // --- Paint definitions ---
            val titlePaint = Paint().apply {
                color = Color.parseColor("#102A43")
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val subheadPaint = Paint().apply {
                color = Color.parseColor("#1E88E5")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val metaPaint = Paint().apply {
                color = Color.parseColor("#627D98")
                textSize = 10f
                isAntiAlias = true
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#102A43")
                textSize = 12f
                isAntiAlias = true
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E0E7F1")
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            val pinnedPaint = Paint().apply {
                color = Color.parseColor("#FFC107")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val bgHeaderPaint = Paint().apply {
                color = Color.parseColor("#E3F2FD")
                style = Paint.Style.FILL
            }

            // Helper: start a new page
            fun newPage() {
                if (currentPage != null) document.finishPage(currentPage)
                pageNumber++
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = document.startPage(pageInfo)
                canvas = currentPage!!.canvas
                currentY = MARGIN
            }

            // Helper: check if we need a new page before writing
            fun ensureSpace(needed: Float) {
                if (canvas == null || currentY + needed > PAGE_HEIGHT - MARGIN) {
                    newPage()
                }
            }

            // Helper: wrap and draw text, returns new Y
            fun drawWrappedText(text: String, paint: Paint, x: Float, startY: Float, maxWidth: Float): Float {
                if (text.isBlank()) return startY
                var y = startY
                val words = text.split(" ")
                var line = ""
                for (word in words) {
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(testLine) > maxWidth) {
                        ensureSpace(paint.textSize + 4f)
                        canvas!!.drawText(line, x, currentY, paint)
                        currentY += paint.textSize + 4f
                        y = currentY
                        line = word
                    } else {
                        line = testLine
                    }
                }
                if (line.isNotEmpty()) {
                    ensureSpace(paint.textSize + 4f)
                    canvas!!.drawText(line, x, currentY, paint)
                    currentY += paint.textSize + 4f
                    y = currentY
                }
                return y
            }

            // ── Cover / Header page ──
            newPage()

            // Blue header bar
            canvas!!.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 140f, bgHeaderPaint)

            currentY = 55f
            canvas!!.drawText(subjectName, MARGIN, currentY, titlePaint)
            currentY += 32f
            canvas!!.drawText("$mode Mode · Notes Export", MARGIN, currentY, subheadPaint)
            currentY += 22f
            val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
            canvas!!.drawText("Generated on $dateStr  ·  ${notes.size} note${if (notes.size == 1) "" else "s"}", MARGIN, currentY, metaPaint)
            currentY = 160f

            // ── Each note ──
            val sortedNotes = notes.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
            sortedNotes.forEachIndexed { index, note ->
                ensureSpace(80f)

                // Note card background tint (light blue strip on left)
                val cardTop = currentY - 8f
                val cardLeft = MARGIN - 12f
                val cardPaint = Paint().apply {
                    color = Color.parseColor("#F1F5F9")
                    style = Paint.Style.FILL
                }
                // We will draw it after measuring height - just draw a left border line
                canvas!!.drawLine(cardLeft, cardTop, cardLeft, cardTop + 200f, subheadPaint.apply {
                    color = Color.parseColor("#1E88E5")
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                })
                subheadPaint.style = Paint.Style.FILL
                subheadPaint.color = Color.parseColor("#1E88E5")

                // Note number badge
                canvas!!.drawText("${index + 1}.", MARGIN, currentY, subheadPaint)

                // Pinned indicator
                if (note.pinned) {
                    canvas!!.drawText("📌 Pinned", MARGIN + 28f, currentY, pinnedPaint)
                    currentY += pinnedPaint.textSize + 6f
                } else {
                    currentY += 4f
                }

                // Title
                if (note.title.isNotBlank()) {
                    ensureSpace(24f)
                    val boldBodyPaint = Paint(bodyPaint).apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textSize = 15f
                        color = Color.parseColor("#102A43")
                    }
                    drawWrappedText(note.title, boldBodyPaint, MARGIN, currentY, CONTENT_WIDTH)
                    currentY += 4f
                }

                // Meta: date
                ensureSpace(16f)
                val noteDateStr = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(note.updatedAt))
                canvas!!.drawText("Last edited: $noteDateStr", MARGIN, currentY, metaPaint)
                currentY += metaPaint.textSize + 8f

                // Content
                if (note.content.isNotBlank()) {
                    // Handle newlines in content
                    val lines = note.content.split("\n")
                    for (line in lines) {
                        if (line.isBlank()) {
                            currentY += bodyPaint.textSize
                        } else {
                            drawWrappedText(line, bodyPaint, MARGIN, currentY, CONTENT_WIDTH)
                        }
                    }
                } else {
                    ensureSpace(bodyPaint.textSize + 4f)
                    val emptyPaint = Paint(bodyPaint).apply { color = Color.parseColor("#627D98") }
                    canvas!!.drawText("(No content)", MARGIN, currentY, emptyPaint)
                    currentY += emptyPaint.textSize + 4f
                }

                currentY += 20f

                // Divider between notes
                if (index < sortedNotes.size - 1) {
                    ensureSpace(16f)
                    canvas!!.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, dividerPaint)
                    currentY += 20f
                }
            }

            // Footer on last page
            ensureSpace(20f)
            val footerPaint = Paint(metaPaint).apply { textSize = 9f; color = Color.parseColor("#A0AEC0") }
            canvas!!.drawText("StudyPilot · $subjectName Notes Export", MARGIN, PAGE_HEIGHT - 30f, footerPaint)

            if (currentPage != null) document.finishPage(currentPage)

            // ── Save to Downloads ──
            val fileName = "${subjectName.replace(" ", "_")}_Notes_${
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            }.pdf"

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StudyPilot")
                }
                val resolver = context.contentResolver
                val insertedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Could not create file in Downloads"))
                resolver.openOutputStream(insertedUri)?.use { out ->
                    document.writeTo(out)
                }
                insertedUri
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "StudyPilot")
                dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out -> document.writeTo(out) }
                Uri.fromFile(file)
            }

            document.close()
            android.util.Log.d("PdfExportService", "PDF exported: $uri")
            Result.success(uri)

        } catch (e: Exception) {
            android.util.Log.e("PdfExportService", "Export failed: ${e.message}", e)
            Result.failure(Exception("Export failed: ${e.message}"))
        }
    }
}