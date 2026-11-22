package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LumiereStrings
import com.example.model.AppLanguage
import com.example.model.WorkspaceMode
import com.example.ui.components.CastAndSettingsSheet
import com.example.ui.components.CharacterCodexDialog
import com.example.ui.components.CinemaReaderScreen
import com.example.ui.components.DirectorsStudioScreen
import com.example.ui.components.LumiereHeader
import com.example.ui.theme.LocalLumiereColors
import com.example.ui.theme.LumiereTheme
import com.example.viewmodel.LumiereViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check wiring with launch intent BEFORE UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            LumiereApp()
        }
    }
}

private sealed interface ImagePickTarget {
    data class Scene(val sceneId: String) : ImagePickTarget
    data class Character(val characterId: String) : ImagePickTarget
}

@Composable
fun LumiereApp(viewModel: LumiereViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val copy = remember(state.language) { LumiereStrings.forLanguage(state.language) }

    var pendingImageTarget by remember { mutableStateOf<ImagePickTarget?>(null) }
    var pendingAudioSceneId by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker for Scene Artwork & Character Portraits
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingImageTarget
        pendingImageTarget = null
        if (uri != null && target != null) {
            when (target) {
                is ImagePickTarget.Scene -> viewModel.attachPickedSceneImage(target.sceneId, uri)
                is ImagePickTarget.Character -> viewModel.attachPickedCharacterImage(target.characterId, uri)
            }
        }
    }

    // Zero-permission SAF Audio Picker for Custom .mp3 / .wav Scene Music
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val sceneId = pendingAudioSceneId
        pendingAudioSceneId = null
        if (uri != null && sceneId != null) {
            viewModel.attachPickedSceneMusic(sceneId, uri)
        }
    }

    // Export Project (.json)
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportProjectToUri(uri)
        }
    }

    // Import Project (.json)
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importProjectFromUri(uri)
        }
    }

    LumiereTheme(
        themeMode = state.themeMode,
        language = state.language
    ) {
        val lumiere = LocalLumiereColors.current

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                LumiereHeader(
                    copy = copy,
                    language = state.language,
                    themeMode = state.themeMode,
                    workspaceMode = state.workspaceMode,
                    onToggleLanguage = viewModel::toggleLanguage,
                    onCycleTheme = viewModel::cycleThemeMode,
                    onSelectWorkspace = viewModel::setWorkspaceMode,
                    onOpenSettings = viewModel::openSettingsSheet
                )

                // Status / Error Toast Banner
                AnimatedVisibility(
                    visible = !state.statusBannerMessage.isNullOrBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val isErr = state.statusBannerIsError
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isErr) MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                        else lumiere.goldAccent.copy(alpha = 0.16f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .border(
                                1.dp,
                                if (isErr) MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                                else lumiere.goldAccent.copy(alpha = 0.5f),
                                RoundedCornerShape(14.dp)
                            )
                            .testTag("status_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isErr) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
                                contentDescription = null,
                                tint = if (isErr) MaterialTheme.colorScheme.error else lumiere.goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.statusBannerMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = viewModel::dismissStatusBanner,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = copy.close,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Dual-Mode Workspace Content
                when (state.workspaceMode) {
                    WorkspaceMode.CINEMA_READER -> {
                        CinemaReaderScreen(
                            state = state,
                            copy = copy,
                            computeCacheKey = viewModel::computeBeatCacheKey,
                            onSelectScene = viewModel::selectScene,
                            onSelectBeat = viewModel::selectAndPlayBeat,
                            onTogglePlayPause = viewModel::toggleCinemaPlayPause,
                            onPrevBeat = viewModel::previousBeat,
                            onNextBeat = viewModel::nextBeat,
                            onOpenCodex = viewModel::openCharacterCodex,
                            onToggleAmbient = viewModel::toggleAmbientSoundscape,
                            onSelectAmbientPreset = viewModel::setActiveSceneAmbientPreset,
                            onTriggerSfx = viewModel::triggerActiveSceneSfx,
                            onVoiceVolumeChange = viewModel::updateVoiceVolume,
                            onMusicVolumeChange = viewModel::updateMusicVolume,
                            onSfxVolumeChange = viewModel::updateSfxVolume,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    WorkspaceMode.DIRECTORS_STUDIO -> {
                        DirectorsStudioScreen(
                            state = state,
                            copy = copy,
                            onProseChange = viewModel::updateDirectorRawProse,
                            onInsertSampleProse = viewModel::insertSampleProse,
                            onDirectWithGemini = viewModel::directSceneWithGemini,
                            onSelectScene = viewModel::selectScene,
                            onUpdateSceneTitle = { sceneId, title ->
                                viewModel.updateScene(sceneId) { s ->
                                    if (state.language == AppLanguage.AR) s.copy(titleAr = title)
                                    else s.copy(titleEn = title)
                                }
                            },
                            onUpdateSceneVisualPrompt = { sceneId, prompt ->
                                viewModel.updateScene(sceneId) { it.copy(visualPrompt = prompt) }
                            },
                            onUpdateSceneSfxPrompt = { sceneId, prompt ->
                                viewModel.updateScene(sceneId) { it.copy(sfxPrompt = prompt) }
                            },
                            onSelectAmbientPreset = viewModel::setActiveSceneAmbientPreset,
                            onGenerateSceneArtwork = viewModel::generateSceneArtwork,
                            onPickSceneImage = { sceneId ->
                                pendingImageTarget = ImagePickTarget.Scene(sceneId)
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onPickSceneAudio = { sceneId ->
                                pendingAudioSceneId = sceneId
                                audioPickerLauncher.launch(arrayOf("audio/mpeg", "audio/wav", "audio/x-wav", "audio/*"))
                            },
                            onClearSceneAudio = viewModel::clearCustomSceneMusic,
                            onGenerateSceneSfx = viewModel::generateSceneSfx,
                            onPlaySceneSfx = viewModel::triggerActiveSceneSfx,
                            onUpdateBeatCharacter = { sceneId, beatId, charId ->
                                val char = state.project.characterById(charId)
                                viewModel.updateBeat(sceneId, beatId) {
                                    it.copy(characterId = charId, isNarration = char.isNarrator)
                                }
                            },
                            onUpdateBeatText = { sceneId, beatId, text ->
                                viewModel.updateBeat(sceneId, beatId) { b ->
                                    if (state.language == AppLanguage.AR) b.copy(textAr = text)
                                    else b.copy(textEn = text)
                                }
                            },
                            onUpdateBeatTag = { sceneId, beatId, tag ->
                                viewModel.updateBeat(sceneId, beatId) { it.copy(expressionTag = tag) }
                            },
                            onAddBeat = viewModel::addBeatToScene,
                            onDeleteBeat = viewModel::deleteBeatFromScene,
                            onExportJson = {
                                exportJsonLauncher.launch("lumiere_novel_project.json")
                            },
                            onImportJson = {
                                importJsonLauncher.launch(arrayOf("application/json", "text/plain"))
                            },
                            onResetDemo = viewModel::resetToDemoChapter,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Full-Screen Cast Manager & Pro Voice Settings Overlay
            if (state.showSettingsSheet) {
                CastAndSettingsSheet(
                    state = state,
                    copy = copy,
                    onDismiss = viewModel::closeSettingsSheet,
                    onSelectTheme = viewModel::setThemeMode,
                    onSelectEngine = viewModel::updateVoiceEngine,
                    onUpdateElevenKey = viewModel::updateElevenLabsApiKey,
                    onSelectElevenModel = viewModel::updateElevenLabsModel,
                    onUpdateCharacter = viewModel::updateCharacter,
                    onAddCharacter = viewModel::addNewCharacter,
                    onTestVoice = viewModel::testCharacterVoice,
                    onPickCharacterImage = { charId ->
                        pendingImageTarget = ImagePickTarget.Character(charId)
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onGenerateCharacterPortrait = viewModel::generateCharacterPortrait
                )
            }

            // Character Codex Luxury Modal
            val codexId = state.codexCharacterId
            if (codexId != null) {
                val codexChar = state.project.characterById(codexId)
                CharacterCodexDialog(
                    character = codexChar,
                    language = state.language,
                    copy = copy,
                    voiceSettings = state.voiceSettings,
                    isTestingVoice = state.testingCharacterId == codexChar.id,
                    onPreviewVoice = { viewModel.testCharacterVoice(codexChar.id) },
                    onDismiss = viewModel::closeCharacterCodex
                )
            }

            // Clean Missing ElevenLabs API Key Guidance Dialog (NEVER falls back to robotic OS TTS)
            if (state.showMissingKeyDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissMissingKeyDialog,
                    containerColor = MaterialTheme.colorScheme.surface,
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = lumiere.goldAccent
                        )
                    },
                    title = {
                        Text(
                            text = copy.missingKeyTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = lumiere.goldAccent
                        )
                    },
                    text = {
                        Text(
                            text = copy.missingKeyBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = viewModel::openSettingsSheet,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = lumiere.goldAccent,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("missing_key_open_settings_button")
                        ) {
                            Text(copy.openSettingsCta)
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = viewModel::switchToGeminiAndPlay,
                                modifier = Modifier.testTag("missing_key_switch_gemini_button")
                            ) {
                                Text(copy.switchToGeminiCta, color = lumiere.goldAccent)
                            }
                            TextButton(onClick = viewModel::dismissMissingKeyDialog) {
                                Text(copy.close)
                            }
                        }
                    }
                )
            }
        }
    }
}
