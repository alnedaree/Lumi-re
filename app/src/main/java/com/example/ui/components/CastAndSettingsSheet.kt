package com.example.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LumiereCopy
import com.example.model.AppLanguage
import com.example.model.CharacterProfile
import com.example.model.ElevenLabsModel
import com.example.model.GEMINI_PREBUILT_VOICES
import com.example.model.LumiereThemeMode
import com.example.model.VoiceEngineSettings
import com.example.model.VoiceEngineType
import com.example.ui.theme.LocalLumiereColors
import com.example.viewmodel.LumiereUiState

private val ELEVEN_VOICE_PRESETS = listOf(
    "George" to "JBFqnCBsd6RMkjVDRZzb",
    "Sarah" to "EXAVITQu4vr4xnSDxMaL",
    "Callum" to "N2lVS1w4EtoT3dr4eOWO",
    "Adam" to "pNInz6obpgDQGcFmaJgB",
    "Charlotte" to "XB0fDUnXU5powFXDhCwa"
)

@Composable
fun CastAndSettingsSheet(
    state: LumiereUiState,
    copy: LumiereCopy,
    onDismiss: () -> Unit,
    onSelectTheme: (LumiereThemeMode) -> Unit,
    onSelectEngine: (VoiceEngineType) -> Unit,
    onUpdateElevenKey: (String) -> Unit,
    onSelectElevenModel: (ElevenLabsModel) -> Unit,
    onUpdateCharacter: (String, (CharacterProfile) -> CharacterProfile) -> Unit,
    onAddCharacter: () -> Unit,
    onTestVoice: (String) -> Unit,
    onPickCharacterImage: (String) -> Unit,
    onGenerateCharacterPortrait: (String) -> Unit
) {
    BackHandler(onBack = onDismiss)

    val lumiere = LocalLumiereColors.current
    var showApiKey by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("cast_settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(lumiere.glassSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = copy.castManagerTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = lumiere.goldAccent,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("settings_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = copy.close,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
            ) {
                // 1. LUXURY THEME SELECTOR
                item(key = "theme_selector") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LumiereThemeMode.entries.forEach { mode ->
                            val selected = state.themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) lumiere.goldAccent.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (selected) lumiere.goldAccent else lumiere.hairlineBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectTheme(mode) }
                                    .testTag("settings_theme_${mode.name.lowercase()}")
                            ) {
                                Text(
                                    text = copy.themeLabel(mode),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // 2. PRO VOICE ENGINE SELECTOR (Engine A: ElevenLabs v4 | Engine B: Gemini 3.8 TTS)
                item(key = "voice_engine_config") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.2.dp, lumiere.goldAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = copy.voiceEngineTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = lumiere.goldAccent
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EngineChoiceCard(
                                    title = copy.engineElevenTitle,
                                    subtitle = copy.engineElevenSubtitle,
                                    selected = state.voiceSettings.selectedEngine == VoiceEngineType.ELEVENLABS_V4,
                                    onClick = { onSelectEngine(VoiceEngineType.ELEVENLABS_V4) },
                                    testTag = "engine_select_elevenlabs",
                                    modifier = Modifier.weight(1f)
                                )
                                EngineChoiceCard(
                                    title = copy.engineGeminiTitle,
                                    subtitle = copy.engineGeminiSubtitle,
                                    selected = state.voiceSettings.selectedEngine == VoiceEngineType.GEMINI_FLASH_TTS,
                                    onClick = { onSelectEngine(VoiceEngineType.GEMINI_FLASH_TTS) },
                                    testTag = "engine_select_gemini",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (state.voiceSettings.selectedEngine == VoiceEngineType.ELEVENLABS_V4) {
                                // Engine A Settings: API Key + eleven_v4 / eleven_v4_turbo selector
                                OutlinedTextField(
                                    value = state.voiceSettings.elevenLabsApiKey,
                                    onValueChange = onUpdateElevenKey,
                                    label = { Text(copy.elevenApiKeyLabel) },
                                    placeholder = { Text(copy.elevenApiKeyPlaceholder) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Key, contentDescription = null, tint = lumiere.goldAccent)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { showApiKey = !showApiKey }) {
                                            Icon(
                                                imageVector = if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = "Toggle key visibility"
                                            )
                                        }
                                    },
                                    visualTransformation = if (showApiKey) VisualTransformation.None
                                    else PasswordVisualTransformation(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("elevenlabs_api_key_input")
                                )

                                Text(
                                    text = copy.elevenModelLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ModelToggleChip(
                                        label = copy.elevenModelV4,
                                        selected = state.voiceSettings.elevenLabsModel == ElevenLabsModel.ELEVEN_V4,
                                        onClick = { onSelectElevenModel(ElevenLabsModel.ELEVEN_V4) },
                                        testTag = "eleven_model_v4",
                                        modifier = Modifier.weight(1f)
                                    )
                                    ModelToggleChip(
                                        label = copy.elevenModelTurbo,
                                        selected = state.voiceSettings.elevenLabsModel == ElevenLabsModel.ELEVEN_V4_TURBO,
                                        onClick = { onSelectElevenModel(ElevenLabsModel.ELEVEN_V4_TURBO) },
                                        testTag = "eleven_model_v4_turbo",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    text = copy.expressiveTagsHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                // Engine B Settings Info: Gemini 3.8 Flash TTS on v1beta via Firebase AI Logic
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(lumiere.goldAccent.copy(alpha = 0.12f))
                                        .border(1.dp, lumiere.goldAccent.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = copy.geminiKeylessNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. STRICT CHARACTER VOICE LOCK ROSTER HEADER
                item(key = "cast_roster_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                                    text = copy.voiceLockLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = lumiere.goldAccent
                                )
                            }
                            Text(
                                text = copy.strictLockNotice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onAddCharacter,
                            modifier = Modifier.testTag("settings_add_character_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(copy.addCharacter, style = MaterialTheme.typography.labelLarge, color = lumiere.goldAccent)
                        }
                    }
                }

                // 4. PER-CHARACTER & NARRATOR VOICE CARDS
                items(state.project.characters, key = { it.id }) { character ->
                    CharacterVoiceCard(
                        character = character,
                        language = state.language,
                        copy = copy,
                        voiceSettings = state.voiceSettings,
                        isTesting = state.testingCharacterId == character.id,
                        isGeneratingPortrait = state.generatingArtworkTargetId == character.id,
                        onUpdate = { transform -> onUpdateCharacter(character.id, transform) },
                        onTestVoice = { onTestVoice(character.id) },
                        onPickImage = { onPickCharacterImage(character.id) },
                        onGeneratePortrait = { onGenerateCharacterPortrait(character.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineChoiceCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val lumiere = LocalLumiereColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) lumiere.goldAccent.copy(alpha = 0.2f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = modifier
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) lumiere.goldAccent else lumiere.hairlineBorder,
                RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModelToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val lumiere = LocalLumiereColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun CharacterVoiceCard(
    character: CharacterProfile,
    language: AppLanguage,
    copy: LumiereCopy,
    voiceSettings: VoiceEngineSettings,
    isTesting: Boolean,
    isGeneratingPortrait: Boolean,
    onUpdate: ((CharacterProfile) -> CharacterProfile) -> Unit,
    onTestVoice: () -> Unit,
    onPickImage: () -> Unit,
    onGeneratePortrait: () -> Unit
) {
    val lumiere = LocalLumiereColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, lumiere.hairlineBorder, RoundedCornerShape(18.dp))
            .testTag("character_voice_card_${character.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Identity Row + Test Voice CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CharacterAvatar(
                        character = character,
                        language = language,
                        size = 54.dp,
                        highlightBorder = character.isNarrator
                    )
                    if (isGeneratingPortrait) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = lumiere.goldAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = character.displayName(language),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (character.isNarrator) copy.narratorBadge else character.displayRole(language),
                        style = MaterialTheme.typography.labelSmall,
                        color = lumiere.goldAccent
                    )
                }

                Button(
                    onClick = onTestVoice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = lumiere.goldAccent,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("test_voice_button_${character.id}")
                ) {
                    if (isTesting) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(copy.stopPreview, style = MaterialTheme.typography.labelMedium)
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(copy.testVoice, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Portrait Actions (Upload or Generate via gemini-nano-banana-2.1)
            if (!character.isNarrator) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(copy.uploadImage, style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = onGeneratePortrait,
                        enabled = !isGeneratingPortrait,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = lumiere.goldAccent, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = copy.generatePortrait,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = lumiere.hairlineBorder)

            // Engine-Specific Voice Parameters
            if (voiceSettings.selectedEngine == VoiceEngineType.ELEVENLABS_V4) {
                OutlinedTextField(
                    value = character.elevenVoiceId,
                    onValueChange = { id -> onUpdate { it.copy(elevenVoiceId = id) } },
                    label = { Text(copy.voiceIdLabel) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Voice ID Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ELEVEN_VOICE_PRESETS.forEach { (presetName, presetId) ->
                        val active = character.elevenVoiceId == presetId
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (active) lumiere.goldAccent.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onUpdate { it.copy(elevenVoiceId = presetId) } }
                        ) {
                            Text(
                                text = presetName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) lumiere.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                VoiceParamSlider(
                    label = copy.stabilityLabel,
                    value = character.elevenStability,
                    onValueChange = { v -> onUpdate { it.copy(elevenStability = v) } }
                )
                VoiceParamSlider(
                    label = copy.similarityLabel,
                    value = character.elevenSimilarityBoost,
                    onValueChange = { v -> onUpdate { it.copy(elevenSimilarityBoost = v) } }
                )
                VoiceParamSlider(
                    label = copy.styleLabel,
                    value = character.elevenStyle,
                    onValueChange = { v -> onUpdate { it.copy(elevenStyle = v) } }
                )
            } else {
                // Engine B: Gemini 3.8 Flash TTS Voice + Structured Acting Style / Tone Metadata
                Text(
                    text = copy.geminiVoiceLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GEMINI_PREBUILT_VOICES.forEach { voiceName ->
                        val selected = character.geminiVoiceName == voiceName
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onUpdate { it.copy(geminiVoiceName = voiceName) } }
                        ) {
                            Text(
                                text = voiceName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = character.displayActingStyle(language),
                    onValueChange = { style ->
                        onUpdate {
                            if (language == AppLanguage.AR) it.copy(geminiActingStyleAr = style)
                            else it.copy(geminiActingStyleEn = style)
                        }
                    },
                    label = { Text(copy.actingStyleLabel) },
                    minLines = 2,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun VoiceParamSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val lumiere = LocalLumiereColors.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = lumiere.goldAccent
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = lumiere.goldAccent,
                activeTrackColor = lumiere.goldAccent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.height(28.dp)
        )
    }
}
