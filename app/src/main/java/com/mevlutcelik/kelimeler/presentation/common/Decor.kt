package com.mevlutcelik.kelimeler.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import kotlin.math.cos
import kotlin.math.sin

/** Kahraman kartların zemini: koyu nane gradyanı. */
val HeroGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0F4F38), Color(0xFF082E21), Color(0xFF051A12)),
    start = Offset.Zero,
    end = Offset.Infinite,
)

/** Sağ üstten vuran yumuşak nane ışığı. */
private val HeroGlow = Brush.radialGradient(
    colors = listOf(PrimaryMint.copy(alpha = 0.22f), Color.Transparent),
    center = Offset(Float.POSITIVE_INFINITY, 0f),
    radius = 900f,
)

/**
 * Sekiz köşeli yıldızlardan (iki kare, biri 45° döndürülmüş) oluşan, İslami geometrik süslemelerden
 * esinlenmiş tekrarlayan desen. Zemin süsü olarak çok düşük opaklıkla çizilir.
 */
@Composable
fun GeometricPattern(
    modifier: Modifier = Modifier,
    color: Color = PrimaryMint.copy(alpha = 0.07f),
    cell: Dp = 44.dp,
) {
    Canvas(modifier) {
        val step = cell.toPx()
        val radius = step * 0.34f
        val stroke = Stroke(width = 1.dp.toPx())
        val square = Path()
        fun drawSquare(cx: Float, cy: Float, startDegrees: Double) {
            square.reset()
            for (i in 0 until 4) {
                val angle = Math.toRadians(startDegrees + i * 90.0)
                val px = cx + (radius * cos(angle)).toFloat()
                val py = cy + (radius * sin(angle)).toFloat()
                if (i == 0) square.moveTo(px, py) else square.lineTo(px, py)
            }
            square.close()
            drawPath(square, color, style = stroke)
        }
        var y = 0f
        var row = 0
        while (y < size.height + step) {
            var x = if (row % 2 == 0) 0f else step / 2
            while (x < size.width + step) {
                // Rub'ül-hizb yıldızı: biri düz, biri 45° döndürülmüş iki kare.
                drawSquare(x, y, 45.0)
                drawSquare(x, y, 0.0)
                drawCircle(color, radius * 0.28f, Offset(x, y), style = stroke)
                x += step
            }
            y += step
            row++
        }
    }
}

/** Gradyan + ışık + desen: ana sayfadaki kart, menü başlığı ve kelime başlığı aynı zemini paylaşır. */
@Composable
fun HeroBackground(shape: Shape, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .clip(shape)
            .background(HeroGradient)
            .background(HeroGlow),
    ) {
        GeometricPattern(Modifier.matchParentSize())
        content()
    }
}

/** Ayar satırları ve istatistikler için yuvarlak, hafif renkli simge rozeti. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = PrimaryMint, size: Dp = 36.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}
