package com.thelastjailer.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thelastjailer.app.data.IllustrationCatalog

/**
 * Real scene art, keyed by [illustrationId] and resolved through [IllustrationCatalog]. Any id
 * without art yet falls back to a plain, clearly-labelled placeholder panel rather than a
 * procedurally drawn scene, so it's obvious at a glance which scenes still need art.
 *
 * Fits the complete composition by default. Story scenes use the asset aspect ratio;
 * bounded portrait cards may retain empty space rather than cutting off their subjects.
 */
@Composable
fun SceneIllustration(
    illustrationId: String,
    modifier: Modifier = Modifier,
    imageAlignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    naturalAspectRatio: Boolean = false
) {
    val drawableId = IllustrationCatalog.get(illustrationId)
    val painter = drawableId?.let { painterResource(id = it) }
    val size = painter?.intrinsicSize
    val ratio = if (size != null && size.width.isFinite() && size.height.isFinite() && size.width > 0 && size.height > 0) size.width / size.height else 16f / 9f
    val frame = if (naturalAspectRatio) modifier.fillMaxWidth().aspectRatio(ratio) else modifier
    Box(modifier = frame.clip(RoundedCornerShape(8.dp)).background(JailerColors.Panel)) {
        if (painter != null) {
            Image(
                painter = painter,
                contentDescription = illustrationId.replace('_', ' '),
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                alignment = imageAlignment
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(JailerColors.Panel)) {
                Text(
                    "ART PENDING: $illustrationId",
                    modifier = Modifier.align(Alignment.Center).padding(12.dp),
                    color = JailerColors.TextPrimary.copy(alpha = .7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
