package com.thelastjailer.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
 */
@Composable
fun SceneIllustration(illustrationId: String, modifier: Modifier = Modifier) {
    val drawableId = IllustrationCatalog.get(illustrationId)
    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        if (drawableId != null) {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = illustrationId.replace('_', ' '),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(JailerColors.Panel)) {
                Text(
                    "ART PENDING — $illustrationId",
                    modifier = Modifier.align(Alignment.Center).padding(12.dp),
                    color = JailerColors.TextPrimary.copy(alpha = .7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}
