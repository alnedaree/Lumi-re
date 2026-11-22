package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.AppLanguage
import com.example.model.CharacterProfile
import com.example.model.NovelScene
import com.example.ui.theme.LocalLumiereColors
import java.io.File

private fun bundledSceneDrawable(key: String?): Int? = when (key) {
    "scene_orient_rain" -> R.drawable.img_scene_orient_rain_1795350758316
    "scene_clocktower_vault" -> R.drawable.img_scene_clocktower_vault_1795350757546
    else -> null
}

private fun bundledCharacterDrawable(key: String?): Int? = when (key) {
    "char_layla" -> R.drawable.img_char_layla_1795350758313
    "char_julian" -> R.drawable.img_char_julian_1795350757722
    else -> null
}

@Composable
fun SceneArtworkImage(
    scene: NovelScene,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val customFile = scene.customArtworkPath?.let { File(it) }?.takeIf { it.exists() }
    val bundledRes = bundledSceneDrawable(scene.bundledArtworkKey)
    val lumiere = LocalLumiereColors.current

    when {
        customFile != null -> {
            AsyncImage(
                model = customFile,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        bundledRes != null -> {
            Image(
                painter = painterResource(id = bundledRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }
        else -> {
            // Atmospheric dark gold-velvet cinema frame for newly directed scenes awaiting artwork
            Box(
                modifier = modifier.background(
                    Brush.radialGradient(
                        colors = listOf(
                            lumiere.goldAccent.copy(alpha = 0.25f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
            )
        }
    }
}

@Composable
fun CharacterAvatar(
    character: CharacterProfile,
    language: AppLanguage,
    size: Dp,
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false
) {
    val lumiere = LocalLumiereColors.current
    val accent = Color(character.accentColorHex)
    val borderColor = if (highlightBorder) lumiere.goldAccent else accent.copy(alpha = 0.65f)
    val borderWidth = if (highlightBorder) 2.dp else 1.2.dp

    val customFile = character.customImagePath?.let { File(it) }?.takeIf { it.exists() }
    val bundledRes = bundledCharacterDrawable(character.bundledPortraitKey)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            customFile != null -> {
                AsyncImage(
                    model = customFile,
                    contentDescription = character.displayName(language),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            bundledRes != null -> {
                Image(
                    painter = painterResource(id = bundledRes),
                    contentDescription = character.displayName(language),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            character.isNarrator -> {
                NarratorCrestArtwork(
                    goldColor = lumiere.goldAccent,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = character.displayName(language).take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = lumiere.goldAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun NarratorCrestArtwork(
    goldColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    goldColor.copy(alpha = 0.28f),
                    Color(0xFF0E121C)
                )
            )
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.34f
            drawCircle(
                color = goldColor.copy(alpha = 0.75f),
                radius = r,
                center = c,
                style = Stroke(width = size.minDimension * 0.045f)
            )
            // 4-point cinema star / compass motif
            val starR = r * 0.68f
            drawLine(goldColor, Offset(c.x, c.y - starR), Offset(c.x, c.y + starR), strokeWidth = 2.5f)
            drawLine(goldColor, Offset(c.x - starR, c.y), Offset(c.x + starR, c.y), strokeWidth = 2.5f)
            drawCircle(color = goldColor, radius = r * 0.18f, center = c)
        }
    }
}
