package com.thelastjailer.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Placeholder art for a scene, keyed by [illustrationId] so a real drawable/painter-backed image
 * pipeline can later replace this Canvas sketch without touching any caller — every caller just
 * passes an id, and the label makes clear this is standing in for real art.
 */
@Composable
fun SceneIllustration(illustrationId: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF26303A), Color(0xFF090B10))))
            val tower = Color(0xFF111821)
            drawRect(tower, Offset(size.width * .08f, size.height * .45f), Size(size.width * .09f, size.height * .35f))
            drawRect(tower, Offset(size.width * .17f, size.height * .34f), Size(size.width * .07f, size.height * .46f))
            drawRect(tower, Offset(size.width * .72f, size.height * .39f), Size(size.width * .08f, size.height * .41f))
            val mountain = Path().apply {
                moveTo(0f, size.height * .68f)
                lineTo(size.width * .28f, size.height * .38f)
                lineTo(size.width * .46f, size.height * .67f)
                lineTo(size.width * .63f, size.height * .43f)
                lineTo(size.width, size.height * .69f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(mountain, Color(0xFF141A20))
            val tree = Color(0xFF0B0D10)
            drawCircle(tree, size.minDimension * .33f, Offset(size.width * .73f, size.height * .25f))
            drawCircle(tree, size.minDimension * .29f, Offset(size.width * .88f, size.height * .38f))
            drawRect(tree, Offset(size.width * .72f, size.height * .22f), Size(size.width * .12f, size.height * .78f))
            val doorLeft = size.width * .63f
            val doorTop = size.height * .30f
            val doorW = size.width * .20f
            val doorH = size.height * .49f
            val radius = CornerRadius(12f, 12f)
            drawRoundRect(color = Color(0xFF17120D), topLeft = Offset(doorLeft, doorTop), size = Size(doorW, doorH), cornerRadius = radius)
            drawRoundRect(color = JailerColors.GoldSoft.copy(alpha = .7f), topLeft = Offset(doorLeft, doorTop), size = Size(doorW, doorH), cornerRadius = radius, style = Stroke(3f))
            drawCircle(color = JailerColors.Gold, radius = 4f, center = Offset(doorLeft + doorW * .76f, doorTop + doorH * .54f))
            drawLine(JailerColors.GoldSoft, Offset(doorLeft + doorW * .12f, doorTop + doorH * .72f), Offset(doorLeft + doorW * .88f, doorTop + doorH * .72f), 2f)
            drawCircle(color = JailerColors.Gold.copy(alpha = .35f), radius = 28f, center = Offset(doorLeft + doorW * .52f, doorTop + doorH * .32f))
            drawCircle(color = JailerColors.Gold, radius = 5f, center = Offset(doorLeft + doorW * .52f, doorTop + doorH * .32f))
            val kx = size.width * .25f
            val ky = size.height * .57f
            drawCircle(color = Color(0xFF0A0B0D), radius = 18f, center = Offset(kx, ky - 47f))
            drawRoundRect(color = Color(0xFF0A0B0D), topLeft = Offset(kx - 25f, ky - 28f), size = Size(50f, 92f), cornerRadius = CornerRadius(16f, 16f))
            drawLine(Color(0xFF6C6B65), Offset(kx + 18f, ky + 12f), Offset(kx + 56f, ky + 54f), 5f, StrokeCap.Round)
            repeat(34) { i ->
                val x = (i * 47f) % size.width
                val y = (i * 71f) % size.height
                drawLine(Color.White.copy(alpha = .10f), Offset(x, y), Offset(x - 8f, y + 22f), 1f)
            }
        }
        Text(
            "PLACEHOLDER ART — $illustrationId",
            modifier = Modifier.padding(12.dp).align(Alignment.BottomStart),
            color = JailerColors.TextPrimary.copy(alpha = .78f),
            fontSize = 10.sp
        )
    }
}
