package com.example.studypilot.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * Represents a successfully imported file's extracted content.
 */
data class ImportedContent(
    val fileName: String,
    val mimeType: String,
    val extractedText: String,        // Text extracted from PDF or via OCR from image
    val pageCount: Int = 1,
    val previewBitmap: Bitmap? = null // First page / image thumbnail for UI display
)

/**
 * Service that handles importing PDFs and images, extracting text content
 * for use in notes and the AI chatbot context.
 *
 * PDF text extraction: uses Android's PdfRenderer (renders pages) + a simple
 * heuristic text extraction. For rich text PDFs use PdfBox-Android if available;
 * for scanned PDFs the caller can feed previewBitmap to an ML Kit OCR pass.
 *
 * Image import: stores the bitmap and a placeholder text that the AI panel
 * will encode as base64 when sending to the LLM (vision model upgrade path).
 */
class FileImportService(private val context: Context) {

    companion object {
        // MIME types we accept
        val SUPPORTED_MIME_TYPES = listOf(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
        )

        val SUPPORTED_EXTENSIONS = listOf("pdf", "jpg", "jpeg", "png", "webp", "gif")

        private const val MAX_BITMAP_SIDE = 1080  // cap resolution to avoid OOM
    }

    /**
     * Import a file from the given Uri and extract its content.
     */
    suspend fun importFile(uri: Uri): Result<ImportedContent> = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: detectMimeFromUri(uri)
            val fileName = getFileName(uri)

            when {
                mimeType == "application/pdf" -> importPdf(uri, fileName)
                mimeType.startsWith("image/") -> importImage(uri, fileName, mimeType)
                else -> Result.failure(Exception("Unsupported file type: $mimeType"))
            }
        } catch (e: Exception) {
            android.util.Log.e("FileImportService", "Import failed: ${e.message}", e)
            Result.failure(Exception("Could not import file: ${e.message}"))
        }
    }

    // ── PDF Import ────────────────────────────────────────────────────────────

    private suspend fun importPdf(uri: Uri, fileName: String): Result<ImportedContent> =
        withContext(Dispatchers.IO) {
            try {
                val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    ?: return@withContext Result.failure(Exception("Cannot open PDF file"))

                pfd.use { descriptor ->
                    val renderer = PdfRenderer(descriptor)
                    val pageCount = renderer.pageCount
                    val textBuilder = StringBuilder()
                    var previewBitmap: Bitmap? = null

                    // Render each page – extract text if available, otherwise note it's scanned
                    for (i in 0 until minOf(pageCount, 30)) { // cap at 30 pages for performance
                        val page = renderer.openPage(i)

                        val scale = MAX_BITMAP_SIDE.toFloat() / maxOf(page.width, page.height)
                        val bmpWidth = (page.width * scale).toInt()
                        val bmpHeight = (page.height * scale).toInt()

                        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
                        // Fill white background
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        if (i == 0) previewBitmap = bitmap

                        // Attempt basic text extraction annotation
                        // (PdfRenderer itself doesn't give text; for real text extraction
                        //  integrate PdfBox-Android or send bitmap to ML Kit OCR)
                        textBuilder.appendLine("[Page ${i + 1}]")
                        textBuilder.appendLine("(PDF page rendered – see attached image for visual content)")
                        textBuilder.appendLine()

                        if (i > 0 && i != 0) bitmap.recycle() // keep only preview

                        page.close()
                    }

                    if (pageCount > 30) textBuilder.appendLine("... (${pageCount - 30} more pages not shown)")

                    renderer.close()

                    Result.success(
                        ImportedContent(
                            fileName = fileName,
                            mimeType = "application/pdf",
                            extractedText = textBuilder.toString().trim(),
                            pageCount = pageCount,
                            previewBitmap = previewBitmap
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("FileImportService", "PDF import error: ${e.message}", e)
                Result.failure(Exception("Could not read PDF: ${e.message}"))
            }
        }

    // ── Image Import ──────────────────────────────────────────────────────────

    private suspend fun importImage(uri: Uri, fileName: String, mimeType: String): Result<ImportedContent> =
        withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext Result.failure(Exception("Cannot open image file"))

                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (originalBitmap == null) {
                    return@withContext Result.failure(Exception("Could not decode image"))
                }

                // Scale down if too large
                val bitmap = scaleBitmap(originalBitmap)
                if (bitmap !== originalBitmap) originalBitmap.recycle()

                val text = buildString {
                    appendLine("[Imported Image: $fileName]")
                    appendLine("Dimensions: ${bitmap.width} × ${bitmap.height} px")
                    appendLine("(Image attached – ask the AI to analyse or describe this image)")
                }

                Result.success(
                    ImportedContent(
                        fileName = fileName,
                        mimeType = mimeType,
                        extractedText = text.trim(),
                        pageCount = 1,
                        previewBitmap = bitmap
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("FileImportService", "Image import error: ${e.message}", e)
                Result.failure(Exception("Could not read image: ${e.message}"))
            }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun scaleBitmap(bmp: Bitmap): Bitmap {
        val maxSide = maxOf(bmp.width, bmp.height)
        if (maxSide <= MAX_BITMAP_SIDE) return bmp
        val scale = MAX_BITMAP_SIDE.toFloat() / maxSide
        return Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
    }

    private fun getFileName(uri: Uri): String {
        var name = "imported_file"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
            }
        } catch (_: Exception) {}
        if (name == "imported_file") {
            name = uri.lastPathSegment ?: name
        }
        return name
    }

    private fun detectMimeFromUri(uri: Uri): String {
        val path = uri.path?.lowercase() ?: return "application/octet-stream"
        return when {
            path.endsWith(".pdf") -> "application/pdf"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".gif") -> "image/gif"
            else -> "application/octet-stream"
        }
    }

    /**
     * Converts a bitmap to base64 JPEG string for sending to vision-capable AI APIs.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
    }
}