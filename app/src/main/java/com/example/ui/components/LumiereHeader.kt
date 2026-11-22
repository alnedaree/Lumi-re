package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LumiereCopy
import com.example.model.AppLanguage
import com.example.model.LumiereThemeMode
import com.example.model.WorkspaceMode
import com.example.ui.theme.LocalLumiereColors

@Composable
fun LumiereHeader(
    copy: LumiereCopy,
    language: AppLanguage,
    themeMode: LumiereThemeMode,
    workspaceMode: WorkspaceMode,
    onToggleLanguage: () -> Unit,
    onCycleTheme: () -> Unit,
    onSelectWorkspace: (WorkspaceMode) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lumiere = LocalLumiereColors.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        lumiere.glassSurface,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.94f)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Row 1: Brand Wordmark + Utility Pills (AR/EN, Theme, Cast & Voice Engine)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = copy.appBrand,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        letterSpacing = if (language == AppLanguage.AR) 0.sp else 4.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = lumiere.goldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = copy.appTagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Instant AR / EN + RTL/LTR Toggle
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier
                        .border(1.dp, lumiere.hairlineBorder, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onToggleLanguage)
                        .testTag("language_toggle_button")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = copy.languageToggleLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = lumiere.goldAccent
                        )
                    }
                }

                // Luxury Theme Cycler (Obsidian / Parchment / Daylight)
                IconButton(
                    onClick = onCycleTheme,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                        .border(1.dp, lumiere.hairlineBorder, CircleShape)
                        .testTag("theme_cycle_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ColorLens,
                        contentDescription = copy.themeLabel(themeMode),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Cast & Voice Engine Settings
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(lumiere.goldAccent.copy(alpha = 0.18f))
                        .border(1.dp, lumiere.goldAccent.copy(alpha = 0.55f), CircleShape)
                        .testTag("open_voice_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RecordVoiceOver,
                        contentDescription = copy.settingsAndCast,
                        tint = lumiere.goldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Dual-Mode Workspace Switcher (Cinema Reader | Director's Studio)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, lumiere.hairlineBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            WorkspaceSegmentButton(
                label = copy.cinemaReader,
                icon = Icons.Outlined.MovieFilter,
                selected = workspaceMode == WorkspaceMode.CINEMA_READER,
                onClick = { onSelectWorkspace(WorkspaceMode.CINEMA_READER) },
                testTag = "mode_cinema_button",
                modifier = Modifier.weight(1f)
            )
            WorkspaceSegmentButton(
                label = copy.directorsStudio,
                icon = Icons.Outlined.AutoAwesome,
                selected = workspaceMode == WorkspaceMode.DIRECTORS_STUDIO,
                onClick = { onSelectWorkspace(WorkspaceMode.DIRECTORS_STUDIO) },
                testTag = "mode_studio_button",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WorkspaceSegmentButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val lumiere = LocalLumiereColors.current
    val bgColor = if (selected) lumiere.goldAccent else Color.Transparent
    val fgColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fgColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
