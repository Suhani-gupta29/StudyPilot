import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class TopWaveCutoutShape(
    private val cornerRadius: Float = 36f,
    private val cutoutRadius: Float = 180f,   // wider than button
    private val cutoutDepth: Float = 130f     // deeper dip
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {

        val w = size.width
        val h = size.height
        val c = w / 2f

        val path = Path().apply {

            // Top-left corner
            moveTo(cornerRadius, 0f)
            quadraticBezierTo(0f, 0f, 0f, cornerRadius)

            // Left side
            lineTo(0f, h)

            // Bottom
            lineTo(w, h)

            // Right side
            lineTo(w, cornerRadius)
            quadraticBezierTo(w, 0f, w - cornerRadius, 0f)

            // Move to right of dip
            lineTo(c + cutoutRadius, 0f)

            // Smooth right curve into dip
            cubicTo(
                c + cutoutRadius * 0.65f, 0f,
                c + cutoutRadius * 0.65f, cutoutDepth,
                c, cutoutDepth
            )

            // Smooth left curve out of dip
            cubicTo(
                c - cutoutRadius * 0.65f, cutoutDepth,
                c - cutoutRadius * 0.65f, 0f,
                c - cutoutRadius, 0f
            )

            close()
        }

        return Outline.Generic(path)
    }
}
