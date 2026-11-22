package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LumiereCopy
import com.example.model.AmbientPreset
import com.example.model.AppLanguage
import com.example.model.CharacterProfile
import com.example.model.DialogueBeat
import com.example.model.ELEVEN_EXPRESSIVE_TAGS
import com.example.ui.theme.LocalLumiereColors
import com.example.viewmodel.LumiereUiState

@Composable
fun DirectorsStudioScreen(
    state: LumiereUiState,
    copy: LumiereCopy,
    onProseChange: (String) -> Unit,
    onInsertSampleProse: () -> Unit,
    onDirectWithGemini: () -> Unit,
    onSelectScene: (Int) -> Unit,
    onUpdateSceneTitle: (String, String) -> Unit,
    onUpdateSceneVisualPrompt: (String, String) -> Unit,
    onUpdateSceneSfxPrompt: (String, String) -> Unit,
    onSelectAmbientPreset: (AmbientPreset) -> Unit,
    onGenerateSceneArtwork: (String) -> Unit,
    onPickSceneImage: (String) -> Unit,
    onPickSceneAudio: (String) -> Unit,
    onClearSceneAudio: (String) -> Unit,
    onGenerateSceneSfx: (String) -> Unit,
    onPlaySceneSfx: () -> Unit,
    onUpdateBeatCharacter: (String, String, String) -> Unit,
    onUpdateBeatText: (String, String, String) -> Unit,
    onUpdateBeatTag: (String, String, String) -> Unit,
    onAddBeat: (String) -> Unit,
    onDeleteBeat: (String, String) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onResetDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lumiere = LocalLumiereColors.current
    val activeScene = state.activeScene

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        // 1. PROJECT PERSISTENCE BAR (Auto-Save + Export JSON + Import JSON + Reset)
        item(key = "persistence_bar") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, lumiere.hairlineBorder, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.project.displayTitle(state.language),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = lumiere.goldAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = copy.autoSavedBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = onResetDemo,
                            modifier = Modifier.testTag("studio_reset_demo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(copy.resetDemo, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportJson,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_export_json_button")
                        ) {
                            Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(copy.exportJson, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onImportJson,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_import_json_button")
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(copy.importJson, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 2. PASTE & DIRECT WITH GEMINI 3.8 FLASH
        item(key = "paste_and_direct") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, lumiere.goldAccent.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = lumiere.goldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = copy.studioPasteTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        TextButton(
                            onClick = onInsertSampleProse,
                            modifier = Modifier.testTag("studio_insert_sample_button")
                        ) {
                            Text(copy.loadSampleProse, style = MaterialTheme.typography.labelMedium, color = lumiere.goldAccent)
                        }
                    }

                    OutlinedTextField(
                        value = state.directorRawProse,
                        onValueChange = onProseChange,
                        placeholder = {
                            Text(
                                text = copy.studioPasteHint,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_raw_prose_input")
                    )

                    Button(
                        onClick = onDirectWithGemini,
                        enabled = !state.isDirectingScene,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = lumiere.goldAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("studio_direct_gemini_button")
                    ) {
                        if (state.isDirectingScene) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(copy.directingProgress, style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(copy.directWithGemini, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // 3. SCENE PRODUCTION EDITOR
        item(key = "scene_production_${activeScene.id}") {
            val isGeneratingArt = state.generatingArtworkTargetId == activeScene.id
            val isGeneratingSfx = state.generatingSfxSceneId == activeScene.id

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, lumiere.hairlineBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Scene Switcher Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.project.scenes.forEachIndexed { idx, s ->
                            val selected = idx == state.activeSceneIndex
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onSelectScene(idx) }
                            ) {
                                Text(
                                    text = "${copy.sceneLabel} ${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Scene Artwork Preview + Loading State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(164.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, lumiere.hairlineBorder, RoundedCornerShape(14.dp))
                    ) {
                        SceneArtworkImage(
                            scene = activeScene,
                            contentDescription = activeScene.displayTitle(state.language),
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isGeneratingArt) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xB80B0D12)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = lumiere.goldAccent)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = copy.generatingArtwork,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFF5EFE6)
                                    )
                                }
                            }
                        }
                    }

                    // Scene Artwork Buttons: Upload OR Generate via gemini-nano-banana-2.1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPickSceneImage(activeScene.id) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("studio_upload_scene_image")
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(copy.uploadImage, style = MaterialTheme.typography.labelMedium)
                        }
                        Button(
                            onClick = { onGenerateSceneArtwork(activeScene.id) },
                            enabled = !isGeneratingArt,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = lumiere.goldAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_generate_scene_artwork")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = copy.generateArtwork,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Scene Title & Visual Prompt Inputs
                    OutlinedTextField(
                        value = activeScene.displayTitle(state.language),
                        onValueChange = { onUpdateSceneTitle(activeScene.id, it) },
                        label = { Text(copy.sceneTitleLabel) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = activeScene.visualPrompt,
                        onValueChange = { onUpdateSceneVisualPrompt(activeScene.id, it) },
                        label = { Text(copy.visualPromptLabel) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = lumiere.hairlineBorder)

                    // Background Music: Procedural Synth Presets + Custom .mp3/.wav Upload
                    Text(
                        text = copy.ambientSoundscape,
                        style = MaterialTheme.typography.titleSmall,
                        color = lumiere.goldAccent
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AmbientPreset.entries.forEach { preset ->
                            val selected = activeScene.ambientPreset == preset
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (selected) lumiere.goldAccent.copy(alpha = 0.24f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .border(
                                        1.dp,
                                        if (selected) lumiere.goldAccent else Color.Transparent,
                                        RoundedCornerShape(50)
                                    )
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onSelectAmbientPreset(preset) }
                            ) {
                                Text(
                                    text = copy.ambientLabel(preset),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onPickSceneAudio(activeScene.id) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("studio_upload_custom_audio")
                        ) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeScene.customMusicName?.take(18) ?: copy.uploadCustomAudio,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (!activeScene.customMusicPath.isNullOrBlank()) {
                            TextButton(onClick = { onClearSceneAudio(activeScene.id) }) {
                                Text(copy.clearCustomAudio, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    HorizontalDivider(color = lumiere.hairlineBorder)

                    // ElevenLabs SFX Generator (eleven_text_to_sound_v3)
                    OutlinedTextField(
                        value = activeScene.sfxPrompt,
                        onValueChange = { onUpdateSceneSfxPrompt(activeScene.id, it) },
                        label = { Text(copy.sfxPromptLabel) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onGenerateSceneSfx(activeScene.id) },
                            enabled = !isGeneratingSfx,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("studio_generate_sfx_button")
                        ) {
                            if (isGeneratingSfx) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(copy.generatingSfx, style = MaterialTheme.typography.labelMedium)
                            } else {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = lumiere.goldAccent, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = copy.generateSfxBtn,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!activeScene.sfxFilePath.isNullOrBlank()) {
                            IconButton(
                                onClick = onPlaySceneSfx,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(lumiere.goldAccent.copy(alpha = 0.2f))
                                    .testTag("studio_play_sfx_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = copy.triggerSfx,
                                    tint = lumiere.goldAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. SCRIPT BEATS SECTION HEADER
        item(key = "beats_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = lumiere.goldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = copy.scriptBeatsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                TextButton(
                    onClick = { onAddBeat(activeScene.id) },
                    modifier = Modifier.testTag("studio_add_beat_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(copy.addBeat, style = MaterialTheme.typography.labelLarge, color = lumiere.goldAccent)
                }
            }
        }

        // 5. INDIVIDUAL VOICE-LOCKED BEAT EDITORS
        itemsIndexed(activeScene.beats, key = { _, b -> "edit_${b.id}" }) { idx, beat ->
            BeatEditorCard(
                index = idx,
                beat = beat,
                characters = state.project.characters,
                language = state.language,
                copy = copy,
                canDelete = activeScene.beats.size > 1,
                onSelectCharacter = { charId -> onUpdateBeatCharacter(activeScene.id, beat.id, charId) },
                onTextChange = { text -> onUpdateBeatText(activeScene.id, beat.id, text) },
                onTagChange = { tag -> onUpdateBeatTag(activeScene.id, beat.id, tag) },
                onDelete = { onDeleteBeat(activeScene.id, beat.id) }
            )
        }
    }
}

@Composable
private fun BeatEditorCard(
    index: Int,
    beat: DialogueBeat,
    characters: List<CharacterProfile>,
    language: AppLanguage,
    copy: LumiereCopy,
    canDelete: Boolean,
    onSelectCharacter: (String) -> Unit,
    onTextChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val lumiere = LocalLumiereColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, lumiere.hairlineBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${copy.beatOf} #${index + 1} · ${copy.speakerLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = lumiere.goldAccent
                )
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete beat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Locked Character Assignment Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                characters.forEach { char ->
                    val selected = char.id == beat.characterId
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) lumiere.goldAccent.copy(alpha = 0.24f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) lumiere.goldAccent else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectCharacter(char.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CharacterAvatar(character = char, language = language, size = 20.dp)
                            Text(
                                text = char.displayName(language),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = beat.displayText(language),
                onValueChange = onTextChange,
                minLines = 2,
                maxLines = 5,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // ElevenLabs v4 Expressive Tag Quick Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = copy.expressionTagLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ELEVEN_EXPRESSIVE_TAGS.forEach { tag ->
                    val active = beat.expressionTag == tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) lumiere.goldAccent.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (active) lumiere.goldAccent else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTagChange(if (active) "" else tag) }
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) lumiere.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
