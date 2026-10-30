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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import kotlin.math.min

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

/** Cami silüetinin çizildiği tasarım ölçüsü (birim); çizim ekrana oranı korunarak ölçeklenir. */
private const val MosqueWidth = 400f
private const val MosqueHeight = 150f

/**
 * Klasik Osmanlı camisi silüeti: merkezî kubbe ve hilalli alem, yarım kubbeler, son cemaat yeri
 * kanatları ve kalem minareler. Alanın dibine oturur, alanın [maxHeightFraction] kadarını ve
 * [maxHeight]'ı geçmeyecek kadar büyür; [horizontalBias] -1 sola, 1 sağa yaslar.
 *
 * Parçalar ayrı bir katmanda opak çizilir, saydamlık katmana topluca uygulanır: üst üste binen
 * parçalar koyulaşmaz, pencereler de katmandan oyulur.
 */
@Composable
fun MosqueSkyline(
    modifier: Modifier = Modifier,
    color: Color = PrimaryMint.copy(alpha = 0.09f),
    maxHeightFraction: Float = 0.5f,
    maxHeight: Dp = Dp.Infinity,
    horizontalBias: Float = 0f,
) {
    Canvas(
        modifier.graphicsLayer {
            alpha = color.alpha
            compositingStrategy = CompositingStrategy.Offscreen
        },
    ) {
        val solid = color.copy(alpha = 1f)
        val height = min(size.height * maxHeightFraction, maxHeight.toPx())
        val scale = min(size.width / MosqueWidth, height / MosqueHeight)
        val left = (size.width - MosqueWidth * scale) * (1 + horizontalBias) / 2
        val top = size.height - MosqueHeight * scale
        drawRect(solid, Offset(0f, size.height - 3 * scale), Size(size.width, 3 * scale))
        withTransform({
            translate(left, top)
            scale(scale, scale, Offset.Zero)
        }) {
            drawMosque(solid)
        }
    }
}

private fun DrawScope.drawMosque(color: Color) {
    fun block(l: Float, t: Float, r: Float, b: Float) = drawRect(color, Offset(l, t), Size(r - l, b - t))

    // Ana gövde, kubbe kasnağı ve hafif sivri merkezî kubbe.
    block(110f, 108f, 290f, 150f)
    block(150f, 88f, 250f, 108f)
    drawPath(
        Path().apply {
            moveTo(144f, 90f)
            cubicTo(144f, 58f, 170f, 40f, 200f, 38f)
            cubicTo(230f, 40f, 256f, 58f, 256f, 90f)
            close()
        },
        color,
    )
    // Yarım kubbeler.
    drawPath(Path().apply { moveTo(112f, 108f); cubicTo(112f, 94f, 128f, 84f, 150f, 84f); lineTo(150f, 108f); close() }, color)
    drawPath(Path().apply { moveTo(288f, 108f); cubicTo(288f, 94f, 272f, 84f, 250f, 84f); lineTo(250f, 108f); close() }, color)
    // Alem: ince direk üstünde hilal.
    block(199f, 22f, 201f, 38f)
    drawCircle(color, 5f, Offset(200f, 17f))
    drawCircle(Color.Black, 4.2f, Offset(202.2f, 15.6f), blendMode = BlendMode.Clear)

    // Son cemaat yeri kanatları ve küçük kubbeleri.
    block(66f, 124f, 110f, 150f)
    block(290f, 124f, 334f, 150f)
    listOf(77f, 95f, 305f, 323f).forEach { cx ->
        drawArc(color, 180f, 180f, useCenter = true, topLeft = Offset(cx - 9f, 115f), size = Size(18f, 18f))
    }

    // İki uzun ve iki kısa kalem minare.
    minaret(color, 52f, top = 14f, balconies = listOf(62f, 92f))
    minaret(color, 348f, top = 14f, balconies = listOf(62f, 92f))
    minaret(color, 110f, top = 56f, balconies = listOf(100f))
    minaret(color, 290f, top = 56f, balconies = listOf(100f))

    // Sivri kemerli pencereler ve revak açıklıkları katmandan oyulur.
    val openings = Path()
    for (i in 0 until 10) addArch(openings, 128f + i * 16f, top = 116f, bottom = 138f, width = 7f)
    for (i in 0 until 8) addArch(openings, 165f + i * 10f, top = 93f, bottom = 104f, width = 4.5f)
    listOf(78f, 94f, 306f, 322f).forEach { addArch(openings, it, top = 131f, bottom = 147f, width = 9f) }
    drawPath(openings, Color.Black, blendMode = BlendMode.Clear)
}

private fun DrawScope.minaret(color: Color, x: Float, top: Float, balconies: List<Float>) {
    val coneBase = top + 26f
    drawRect(color, Offset(x - 6f, 128f), Size(12f, 22f)) // kaide
    drawRect(color, Offset(x - 4f, coneBase), Size(8f, 128f - coneBase)) // gövde
    balconies.forEach { y -> drawRect(color, Offset(x - 6.5f, y), Size(13f, 3f)) } // şerefeler
    drawPath(Path().apply { moveTo(x - 4.5f, coneBase); lineTo(x, top); lineTo(x + 4.5f, coneBase); close() }, color) // külah
    drawRect(color, Offset(x - 0.6f, top - 6f), Size(1.2f, 6f)) // alem
}

/** [path]'e tabanı düz, tepesi sivri bir kemer ekler. */
private fun addArch(path: Path, cx: Float, top: Float, bottom: Float, width: Float) {
    val half = width / 2
    val spring = top + width * 0.9f
    val shoulder = top + (spring - top) * 0.3f
    path.moveTo(cx - half, bottom)
    path.lineTo(cx - half, spring)
    path.quadraticTo(cx - half, shoulder, cx, top)
    path.quadraticTo(cx + half, shoulder, cx + half, spring)
    path.lineTo(cx + half, bottom)
    path.close()
}

/**
 * Mihrap: çift çizgili sivri kemer ve tepesinden sarkan kandil. Çalışma kartının önünde kelimeyi
 * bir mihrabın içindeymiş gibi çevreler; kemerin altı açıktır.
 */
@Composable
fun MihrabFrame(modifier: Modifier = Modifier, color: Color = PrimaryMint.copy(alpha = 0.10f)) {
    Canvas(modifier) {
        val stroke = Stroke(width = 1.2.dp.toPx())
        val inset = 8.dp.toPx()
        drawPath(mihrabPath(0f, size.width, 0f, size.height), color, style = stroke)
        val innerTop = inset * 1.8f
        drawPath(mihrabPath(inset, size.width - inset, innerTop, size.height), color.copy(alpha = color.alpha * 0.6f), style = stroke)

        val cx = size.width / 2
        val lampTop = innerTop + 22.dp.toPx()
        drawLine(color, Offset(cx, innerTop), Offset(cx, lampTop), strokeWidth = 1.dp.toPx())
        val w = 8.dp.toPx()
        val h = 14.dp.toPx()
        drawCircle(color, 2.dp.toPx(), Offset(cx, lampTop + 1.dp.toPx()))
        drawPath(
            Path().apply {
                moveTo(cx - w, lampTop + 4.dp.toPx())
                lineTo(cx + w, lampTop + 4.dp.toPx())
                quadraticTo(cx + w, lampTop + h, cx, lampTop + h + 2.dp.toPx())
                quadraticTo(cx - w, lampTop + h, cx - w, lampTop + 4.dp.toPx())
                close()
            },
            color,
        )
    }
}

private fun mihrabPath(left: Float, right: Float, top: Float, bottom: Float): Path {
    val cx = (left + right) / 2
    val spring = top + (right - left) * 0.62f
    return Path().apply {
        moveTo(left, bottom)
        lineTo(left, spring)
        cubicTo(left, spring - (spring - top) * 0.55f, cx - (cx - left) * 0.25f, top + (spring - top) * 0.12f, cx, top)
        cubicTo(cx + (right - cx) * 0.25f, top + (spring - top) * 0.12f, right, spring - (spring - top) * 0.55f, right, spring)
        lineTo(right, bottom)
    }
}

/**
 * Gradyan + ışık + cami silüeti: ana sayfadaki kart, menü başlığı ve kelime başlığı aynı zemini
 * paylaşır. Silüet [skylineBias] ile yana yaslanır, [skylineMaxHeight] ile küçültülür; böylece
 * yazının ya da butonun arkasında kalmaz.
 */
@Composable
fun HeroBackground(
    shape: Shape,
    modifier: Modifier = Modifier,
    skylineBias: Float = 0f,
    skylineMaxHeight: Dp = Dp.Infinity,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .clip(shape)
            .background(HeroGradient)
            .background(HeroGlow),
    ) {
        MosqueSkyline(Modifier.matchParentSize(), maxHeight = skylineMaxHeight, horizontalBias = skylineBias)
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
