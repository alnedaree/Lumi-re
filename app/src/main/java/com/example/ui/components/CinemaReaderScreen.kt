package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LumiereCopy
import com.example.model.AmbientPreset
import com.example.model.AppLanguage
import com.example.model.CharacterProfile
import com.example.model.DialogueBeat
import com.example.model.NovelScene
import com.example.ui.theme.LocalLumiereColors
import com.example.viewmodel.LumiereUiState

@Composable
fun CinemaReaderScreen(
    state: LumiereUiState,
    copy: LumiereCopy,
    computeCacheKey: (DialogueBeat) -> String,
    onSelectScene: (Int) -> Unit,
    onSelectBeat: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevBeat: () -> Unit,
    onNextBeat: () -> Unit,
    onOpenCodex: (String) -> Unit,
    onToggleAmbient: () -> Unit,
    onSelectAmbientPreset: (AmbientPreset) -> Unit,
    onTriggerSfx: () -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val lumiere = LocalLumiereColors.current
    val activeScene = state.activeScene
    val listState = rememberLazyListState()
    var mixerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(activeScene.id, state.activeBeatIndex) {
        if (state.activeBeatIndex in activeScene.beats.indices) {
            listState.animateScrollToItem(state.activeBeatIndex)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. CINEMATIC VISUAL STAGE WITH KEN BURNS & CROSSFADE
            CinematicVisualStage(
                scene = activeScene,
                language = state.language,
                copy = copy,
                isAmbientEnabled = state.isAmbientEnabled,
                onToggleAmbient = onToggleAmbient,
                onTriggerSfx = onTriggerSfx
            )

            // 2. SCENE SELECTOR RIBBON
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.project.scenes.forEachIndexed { idx, scene ->
                    val selected = idx == state.activeSceneIndex
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) lumiere.goldAccent.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .border(
                                1.dp,
                                if (selected) lumiere.goldAccent else lumiere.hairlineBorder,
                                RoundedCornerShape(50)
                            )
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectScene(idx) }
                            .testTag("scene_chip_$idx")
                    ) {
                        Text(
                            text = "${copy.sceneLabel} ${idx + 1} · ${scene.displayTitle(state.language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // 3. SYNCED SCRIPT READER
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 6.dp,
                    bottom = 220.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(activeScene.beats, key = { _, beat -> beat.id }) { index, beat ->
                    val speaker = state.project.characterById(beat.characterId)
                    val isActive = index == state.activeBeatIndex
                    val isCached = computeCacheKey(beat) in state.cachedCacheKeys

                    ScriptBeatCard(
                        beat = beat,
                        speaker = speaker,
                        language = state.language,
                        copy = copy,
                        isActive = isActive,
                        isPlayingNow = isActive && (state.isVoicePlaying || state.isSynthesizingVoice),
                        isCached = isCached,
                        onBeatClick = { onSelectBeat(index) },
                        onSpeakerClick = { onOpenCodex(speaker.id) }
                    )
                }
            }
        }

        // 4. FLOATING ACTIVE SPEAKER PORTRAIT + LUXURY CINEMA CONTROL BAR
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Sleek Floating Active Speaker Portrait + Live Waveform
            ActiveSpeakerFloatingPill(
                speaker = state.activeSpeaker,
                activeBeat = state.activeBeat,
                language = state.language,
                copy = copy,
                isPlaying = state.isVoicePlaying,
                isSynthesizing = state.isSynthesizingVoice,
                waveformLevels = state.waveformLevels,
                onClickSpeaker = { onOpenCodex(state.activeSpeaker.id) }
            )

            // Floating Cinema Control Bar + Expandable 3-Channel Mixer
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = lumiere.glassSurface,
                tonalElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, lumiere.goldAccent.copy(alpha = 0.42f), RoundedCornerShape(22.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    // Expandable 3-Channel Soundscape & SFX Mixer
                    AnimatedVisibility(
                        visible = mixerExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        CinemaMixerPanel(
                            state = state,
                            copy = copy,
                            onSelectAmbientPreset = onSelectAmbientPreset,
                            onVoiceVolumeChange = onVoiceVolumeChange,
                            onMusicVolumeChange = onMusicVolumeChange,
                            onSfxVolumeChange = onSfxVolumeChange
                        )
                    }

                    // Primary Cinema Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Beat Progress Counter
                        Column(modifier = Modifier.width(68.dp)) {
                            Text(
                                text = "${copy.beatOf} ${state.activeBeatIndex + 1}/${activeScene.beats.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = lumiere.goldAccent,
                                maxLines = 1
                            )
                            Text(
                                text = if (activeScene.customMusicName != null) copy.customTrackActive
                                else copy.ambientLabel(activeScene.ambientPreset),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Transport Buttons (Prev · Play/Pause · Next)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = onPrevBeat,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                    .testTag("cinema_prev_beat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = copy.previousBeat,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                lumiere.goldAccent,
                                                lumiere.goldMuted
                                            )
                                        )
                                    )
                                    .clickable(onClick = onTogglePlayPause)
                                    .testTag("cinema_play_pause_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    state.isSynthesizingVoice -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.5.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    state.isCinemaAutoPlaying || state.isVoicePlaying -> {
                                        Icon(
                                            imageVector = Icons.Filled.Pause,
                                            contentDescription = copy.pauseCinema,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Filled.PlayArrow,
                                            contentDescription = copy.playCinema,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = onNextBeat,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                    .testTag("cinema_next_beat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = copy.nextBeat,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Mixer Expand Toggle Button
                        IconButton(
                            onClick = { mixerExpanded = !mixerExpanded },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (mixerExpanded) lumiere.goldAccent.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                                .border(
                                    1.dp,
                                    if (mixerExpanded) lumiere.goldAccent else Color.Transparent,
                                    CircleShape
                                )
                                .testTag("cinema_mixer_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = copy.mixerTitle,
                                tint = if (mixerExpanded) lumiere.goldAccent else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CinematicVisualStage(
    scene: NovelScene,
    language: AppLanguage,
    copy: LumiereCopy,
    isAmbientEnabled: Boolean,
    onToggleAmbient: () -> Unit,
    onTriggerSfx: () -> Unit
) {
    val lumiere = LocalLumiereColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "ken_burns")

    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_scale"
    )
    val kenBurnsPanX by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "kb_pan_x"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(212.dp)
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
    ) {
        Crossfade(
            targetState = scene,
            animationSpec = tween(durationMillis = 700),
            label = "scene_crossfade"
        ) { targetScene ->
            SceneArtworkImage(
                scene = targetScene,
                contentDescription = targetScene.displayTitle(language),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = kenBurnsScale
                        scaleY = kenBurnsScale
                        translationX = kenBurnsPanX
                    }
            )
        }

        // Cinematic chiaroscuro vignette gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to lumiere.stageOverlayTop.copy(alpha = 0.55f),
                        0.45f to Color.Transparent,
                        1.0f to lumiere.stageOverlayBottom.copy(alpha = 0.96f)
                    )
                )
        )

        // Top Stage Soundscape & SFX Quick Pills
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xB80B0D12),
                modifier = Modifier
                    .border(
                        1.dp,
                        if (isAmbientEnabled) lumiere.goldAccent else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(50)
                    )
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToggleAmbient)
                    .testTag("stage_ambient_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isAmbientEnabled) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                        contentDescription = copy.ambientSoundscape,
                        tint = if (isAmbientEnabled) lumiere.goldAccent else Color(0xFFD0C8BC),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = scene.customMusicName?.take(14) ?: copy.ambientLabel(scene.ambientPreset),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF5EFE6)
                    )
                }
            }

            if (!scene.sfxFilePath.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xC42B2412),
                    modifier = Modifier
                        .border(1.dp, lumiere.goldAccent.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onTriggerSfx)
                        .testTag("stage_sfx_trigger")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = copy.triggerSfx,
                            tint = lumiere.goldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = copy.triggerSfx,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF5EFE6)
                        )
                    }
                }
            }
        }

        // Bottom Scene Title Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (scene.displaySubtitle(language).isNotBlank()) {
                Text(
                    text = scene.displaySubtitle(language),
                    style = MaterialTheme.typography.labelSmall,
                    color = lumiere.goldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = scene.displayTitle(language),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ScriptBeatCard(
    beat: DialogueBeat,
    speaker: CharacterProfile,
    language: AppLanguage,
    copy: LumiereCopy,
    isActive: Boolean,
    isPlayingNow: Boolean,
    isCached: Boolean,
    onBeatClick: () -> Unit,
    onSpeakerClick: () -> Unit
) {
    val lumiere = LocalLumiereColors.current
    val speakerAccent = Color(speaker.accentColorHex)

    val containerColor = when {
        isActive -> lumiere.activeBeatGlow
        beat.isNarration -> MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    }

    val borderColor = when {
        isActive -> lumiere.goldAccent
        else -> lumiere.hairlineBorder
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onBeatClick)
            .testTag("script_beat_card_${beat.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Clickable Character Identity -> Opens Character Codex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSpeakerClick)
                        .padding(end = 8.dp)
                        .testTag("beat_speaker_${speaker.id}")
                ) {
                    CharacterAvatar(
                        character = speaker,
                        language = language,
                        size = 32.dp,
                        highlightBorder = isActive
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = speaker.displayName(language),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (beat.isNarration) lumiere.goldAccent else speakerAccent
                        )
                        if (beat.isNarration) {
                            Text(
                                text = copy.narratorBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Badges: Expressive Tag + Cached + Playing indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (beat.expressionTag.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = beat.expressionTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isCached) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(lumiere.goldAccent.copy(alpha = 0.16f))
                                .border(0.8.dp, lumiere.goldAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = copy.cachedBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = lumiere.goldAccent
                            )
                        }
                    }
                    if (isPlayingNow) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = copy.activeSpeaker,
                            tint = lumiere.goldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val displayed = beat.displayText(language)
            Text(
                text = if (beat.isNarration) displayed else "“$displayed”",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = if (beat.isNarration) FontStyle.Italic else FontStyle.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ActiveSpeakerFloatingPill(
    speaker: CharacterProfile,
    activeBeat: DialogueBeat?,
    language: AppLanguage,
    copy: LumiereCopy,
    isPlaying: Boolean,
    isSynthesizing: Boolean,
    waveformLevels: List<Float>,
    onClickSpeaker: () -> Unit
) {
    val lumiere = LocalLumiereColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = lumiere.glassSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isPlaying || isSynthesizing) lumiere.goldAccent else lumiere.hairlineBorder,
                RoundedCornerShape(18.dp)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClickSpeaker)
            .testTag("active_speaker_portrait_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CharacterAvatar(
                character = speaker,
                language = language,
                size = 42.dp,
                highlightBorder = isPlaying || isSynthesizing
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSynthesizing) copy.synthesizingVoice else copy.activeSpeaker,
                    style = MaterialTheme.typography.labelSmall,
                    color = lumiere.goldAccent,
                    maxLines = 1
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = speaker.displayName(language),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!activeBeat?.expressionTag.isNullOrBlank()) {
                        Text(
                            text = activeBeat?.expressionTag.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Animated Gold Audio Waveform
            AudioWaveformBars(
                levels = waveformLevels,
                isActive = isPlaying,
                barColor = lumiere.goldAccent
            )
        }
    }
}

@Composable
private fun AudioWaveformBars(
    levels: List<Float>,
    isActive: Boolean,
    barColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier
            .height(28.dp)
            .padding(start = 8.dp)
    ) {
        levels.take(12).forEach { rawLevel ->
            val target = if (isActive) rawLevel else 0.16f
            val animatedHeight by animateFloatAsState(
                targetValue = target,
                animationSpec = tween(durationMillis = 85),
                label = "wave_bar"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((26f * animatedHeight).dp.coerceAtLeast(4.dp))
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) barColor else barColor.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun CinemaMixerPanel(
    state: LumiereUiState,
    copy: LumiereCopy,
    onSelectAmbientPreset: (AmbientPreset) -> Unit,
    onVoiceVolumeChange: (Float) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onSfxVolumeChange: (Float) -> Unit
) {
    val lumiere = LocalLumiereColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = copy.mixerTitle.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = lumiere.goldAccent
        )

        // Ambient Soundscape Quick Presets
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AmbientPreset.entries.forEach { preset ->
                val selected = state.activeScene.ambientPreset == preset
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (selected) lumiere.goldAccent.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .border(
                            1.dp,
                            if (selected) lumiere.goldAccent else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectAmbientPreset(preset) }
                        .testTag("mixer_ambient_${preset.name.lowercase()}")
                ) {
                    Text(
                        text = copy.ambientLabel(preset),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) lumiere.goldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 3 Independent Volume Sliders
        MixerVolumeRow(
            label = copy.characterVoices,
            icon = Icons.Outlined.VolumeUp,
            value = state.mixer.voiceVolume,
            onValueChange = onVoiceVolumeChange,
            testTag = "mixer_slider_voices"
        )
        MixerVolumeRow(
            label = copy.backgroundMusic,
            icon = Icons.Filled.MusicNote,
            value = state.mixer.musicVolume,
            onValueChange = onMusicVolumeChange,
            testTag = "mixer_slider_music"
        )
        MixerVolumeRow(
            label = copy.soundEffects,
            icon = Icons.Filled.Bolt,
            value = state.mixer.sfxVolume,
            onValueChange = onSfxVolumeChange,
            testTag = "mixer_slider_sfx"
        )
    }
}

@Composable
private fun MixerVolumeRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    val lumiere = LocalLumiereColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = lumiere.goldAccent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(106.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = lumiere.goldAccent,
                activeTrackColor = lumiere.goldAccent,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .testTag(testTag)
        )
        Text(
            text = "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(34.dp)
        )
    }
}
