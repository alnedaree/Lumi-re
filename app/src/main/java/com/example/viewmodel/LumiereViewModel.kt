package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CachedAudioClip
import com.example.audio.LumiereAudioEngine
import com.example.data.DemoNovelSeed
import com.example.data.ProjectStorageRepository
import com.example.model.AmbientPreset
import com.example.model.AppLanguage
import com.example.model.CharacterProfile
import com.example.model.DialogueBeat
import com.example.model.ElevenLabsModel
import com.example.model.LumiereThemeMode
import com.example.model.MixerState
import com.example.model.NovelProject
import com.example.model.NovelScene
import com.example.model.VoiceEngineSettings
import com.example.model.VoiceEngineType
import com.example.model.WorkspaceMode
import com.example.service.ElevenLabsService
import com.example.service.GeminiDirectorAndMediaService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "LumiereViewModel"

data class LumiereUiState(
    val project: NovelProject = DemoNovelSeed.createDemoProject(),
    val language: AppLanguage = AppLanguage.EN,
    val themeMode: LumiereThemeMode = LumiereThemeMode.OBSIDIAN_CINEMA,
    val workspaceMode: WorkspaceMode = WorkspaceMode.CINEMA_READER,
    val voiceSettings: VoiceEngineSettings = VoiceEngineSettings(),
    val mixer: MixerState = MixerState(),
    val activeSceneIndex: Int = 0,
    val activeBeatIndex: Int = 0,
    val isCinemaAutoPlaying: Boolean = false,
    val isVoicePlaying: Boolean = false,
    val isSynthesizingVoice: Boolean = false,
    val isAmbientEnabled: Boolean = false,
    val testingCharacterId: String? = null,
    val cachedCacheKeys: Set<String> = emptySet(),
    val waveformLevels: List<Float> = List(16) { 0.12f },
    val showSettingsSheet: Boolean = false,
    val codexCharacterId: String? = null,
    val showMissingKeyDialog: Boolean = false,
    val statusBannerMessage: String? = null,
    val statusBannerIsError: Boolean = false,
    // Director's Studio state
    val directorRawProse: String = "",
    val isDirectingScene: Boolean = false,
    val generatingArtworkTargetId: String? = null,
    val generatingSfxSceneId: String? = null
) {
    val activeScene: NovelScene
        get() = project.scenes.getOrElse(activeSceneIndex) { project.scenes.first() }

    val activeBeat: DialogueBeat?
        get() = activeScene.beats.getOrNull(activeBeatIndex)

    val activeSpeaker: CharacterProfile
        get() {
            val beat = activeBeat
            return if (beat != null) project.characterById(beat.characterId)
            else project.characters.first()
        }
}

class LumiereViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = ProjectStorageRepository(application.applicationContext)
    private val audioEngine = LumiereAudioEngine(application.applicationContext)
    private val elevenLabsService = ElevenLabsService()
    private val geminiService = GeminiDirectorAndMediaService()

    private val _uiState = MutableStateFlow(
        LumiereUiState(
            project = storage.loadProject(),
            language = storage.loadLanguage(),
            themeMode = storage.loadThemeMode(),
            voiceSettings = storage.loadVoiceSettings(),
            mixer = storage.loadMixerState()
        )
    )
    val uiState: StateFlow<LumiereUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var previewJob: Job? = null

    init {
        val m = _uiState.value.mixer
        audioEngine.setVolumes(m.voiceVolume, m.musicVolume, m.sfxVolume)

        viewModelScope.launch {
            audioEngine.isVoicePlaying.collect { playing ->
                _uiState.update { it.copy(isVoicePlaying = playing) }
            }
        }
        viewModelScope.launch {
            audioEngine.waveformLevels.collect { levels ->
                _uiState.update { it.copy(waveformLevels = levels) }
            }
        }
        viewModelScope.launch {
            audioEngine.cache.cachedKeys.collect { keys ->
                _uiState.update { it.copy(cachedCacheKeys = keys) }
            }
        }
    }

    // --- UI & Navigation Controls ---

    fun toggleLanguage() {
        val next = if (_uiState.value.language == AppLanguage.EN) AppLanguage.AR else AppLanguage.EN
        storage.saveLanguage(next)
        _uiState.update { it.copy(language = next) }
    }

    fun cycleThemeMode() {
        val modes = LumiereThemeMode.entries
        val next = modes[(_uiState.value.themeMode.ordinal + 1) % modes.size]
        setThemeMode(next)
    }

    fun setThemeMode(mode: LumiereThemeMode) {
        storage.saveThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setWorkspaceMode(mode: WorkspaceMode) {
        if (mode == WorkspaceMode.DIRECTORS_STUDIO) {
            pauseCinemaPlayback()
        }
        _uiState.update { it.copy(workspaceMode = mode) }
    }

    fun openSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = true, showMissingKeyDialog = false) }
    }

    fun closeSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    fun openCharacterCodex(characterId: String) {
        _uiState.update { it.copy(codexCharacterId = characterId) }
    }

    fun closeCharacterCodex() {
        previewJob?.cancel()
        audioEngine.stopVoice()
        _uiState.update { it.copy(codexCharacterId = null, testingCharacterId = null) }
    }

    fun dismissMissingKeyDialog() {
        _uiState.update { it.copy(showMissingKeyDialog = false) }
    }

    fun dismissStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null, statusBannerIsError = false) }
    }

    fun switchToGeminiAndPlay() {
        updateVoiceEngine(VoiceEngineType.GEMINI_FLASH_TTS)
        _uiState.update { it.copy(showMissingKeyDialog = false) }
        toggleCinemaPlayPause()
    }

    // --- Voice Engine Settings ---

    fun updateVoiceEngine(engine: VoiceEngineType) {
        val updated = _uiState.value.voiceSettings.copy(selectedEngine = engine)
        storage.saveVoiceSettings(updated)
        _uiState.update { it.copy(voiceSettings = updated) }
    }

    fun updateElevenLabsApiKey(key: String) {
        val updated = _uiState.value.voiceSettings.copy(elevenLabsApiKey = key)
        storage.saveVoiceSettings(updated)
        _uiState.update { it.copy(voiceSettings = updated) }
    }

    fun updateElevenLabsModel(model: ElevenLabsModel) {
        val updated = _uiState.value.voiceSettings.copy(elevenLabsModel = model)
        storage.saveVoiceSettings(updated)
        _uiState.update { it.copy(voiceSettings = updated) }
    }

    // --- Soundscape & 3-Channel Mixer ---

    fun updateVoiceVolume(value: Float) {
        val next = _uiState.value.mixer.copy(voiceVolume = value.coerceIn(0f, 1f))
        applyMixer(next)
    }

    fun updateMusicVolume(value: Float) {
        val next = _uiState.value.mixer.copy(musicVolume = value.coerceIn(0f, 1f))
        applyMixer(next)
    }

    fun updateSfxVolume(value: Float) {
        val next = _uiState.value.mixer.copy(sfxVolume = value.coerceIn(0f, 1f))
        applyMixer(next)
    }

    private fun applyMixer(mixer: MixerState) {
        storage.saveMixerState(mixer)
        audioEngine.setVolumes(mixer.voiceVolume, mixer.musicVolume, mixer.sfxVolume)
        _uiState.update { it.copy(mixer = mixer) }
    }

    fun toggleAmbientSoundscape() {
        val currentlyOn = _uiState.value.isAmbientEnabled
        if (currentlyOn) {
            audioEngine.stopAmbient()
            _uiState.update { it.copy(isAmbientEnabled = false) }
        } else {
            val scene = _uiState.value.activeScene
            audioEngine.startAmbient(scene.ambientPreset, scene.customMusicPath)
            _uiState.update { it.copy(isAmbientEnabled = true) }
        }
    }

    fun setActiveSceneAmbientPreset(preset: AmbientPreset) {
        val scene = _uiState.value.activeScene
        updateScene(scene.id) { it.copy(ambientPreset = preset) }
        if (preset == AmbientPreset.NONE) {
            audioEngine.stopAmbient()
            _uiState.update { it.copy(isAmbientEnabled = false) }
        } else {
            audioEngine.startAmbient(preset, scene.customMusicPath)
            _uiState.update { it.copy(isAmbientEnabled = true) }
        }
    }

    fun triggerActiveSceneSfx() {
        val sfxPath = _uiState.value.activeScene.sfxFilePath
        if (sfxPath.isNullOrBlank()) {
            showBanner(
                if (_uiState.value.language == AppLanguage.AR)
                    "لا يوجد مؤثر صوتي لهذا المشهد بعد. يمكنك توليده في الاستوديو عبر eleven_text_to_sound_v3."
                else
                    "No SFX file attached yet. Generate one in Director's Studio using eleven_text_to_sound_v3.",
                isError = false
            )
            return
        }
        audioEngine.playSfxFile(sfxPath) { err ->
            showBanner(err, isError = true)
        }
    }

    // --- Cinema Reader Playback & Navigation ---

    fun selectScene(index: Int) {
        val scenes = _uiState.value.project.scenes
        if (index !in scenes.indices) return
        val wasAutoPlaying = _uiState.value.isCinemaAutoPlaying
        stopCurrentBeatPlaybackOnly()
        _uiState.update { it.copy(activeSceneIndex = index, activeBeatIndex = 0) }
        val newScene = scenes[index]
        if (_uiState.value.isAmbientEnabled) {
            audioEngine.startAmbient(newScene.ambientPreset, newScene.customMusicPath)
        }
        if (wasAutoPlaying) {
            playBeatAt(index, 0, continueAutoPlay = true)
        }
    }

    fun selectAndPlayBeat(beatIndex: Int) {
        val sceneIdx = _uiState.value.activeSceneIndex
        val scene = _uiState.value.activeScene
        if (beatIndex !in scene.beats.indices) return
        _uiState.update { it.copy(activeBeatIndex = beatIndex) }
        playBeatAt(sceneIdx, beatIndex, continueAutoPlay = _uiState.value.isCinemaAutoPlaying)
    }

    fun toggleCinemaPlayPause() {
        val state = _uiState.value
        if (state.isCinemaAutoPlaying || state.isVoicePlaying || state.isSynthesizingVoice) {
            pauseCinemaPlayback()
        } else {
            // Check ElevenLabs key upfront before starting Cinema playback — NEVER fallback to OS TTS
            if (state.voiceSettings.selectedEngine == VoiceEngineType.ELEVENLABS_V4 &&
                state.voiceSettings.elevenLabsApiKey.isBlank()
            ) {
                _uiState.update { it.copy(showMissingKeyDialog = true) }
                return
            }
            val scene = state.activeScene
            if (scene.ambientPreset != AmbientPreset.NONE || !scene.customMusicPath.isNullOrBlank()) {
                audioEngine.startAmbient(scene.ambientPreset, scene.customMusicPath)
                _uiState.update { it.copy(isAmbientEnabled = true) }
            }
            _uiState.update { it.copy(isCinemaAutoPlaying = true) }
            playBeatAt(state.activeSceneIndex, state.activeBeatIndex, continueAutoPlay = true)
        }
    }

    fun pauseCinemaPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        audioEngine.stopVoice()
        _uiState.update {
            it.copy(
                isCinemaAutoPlaying = false,
                isSynthesizingVoice = false,
                isVoicePlaying = false
            )
        }
    }

    fun nextBeat() {
        val state = _uiState.value
        val scenes = state.project.scenes
        val currentScene = state.activeScene
        val wasAutoPlaying = state.isCinemaAutoPlaying
        val nextBeatIdx: Int
        val nextSceneIdx: Int
        if (state.activeBeatIndex + 1 < currentScene.beats.size) {
            nextSceneIdx = state.activeSceneIndex
            nextBeatIdx = state.activeBeatIndex + 1
        } else if (state.activeSceneIndex + 1 < scenes.size) {
            nextSceneIdx = state.activeSceneIndex + 1
            nextBeatIdx = 0
        } else {
            pauseCinemaPlayback()
            return
        }

        stopCurrentBeatPlaybackOnly()
        _uiState.update { it.copy(activeSceneIndex = nextSceneIdx, activeBeatIndex = nextBeatIdx) }
        if (nextSceneIdx != state.activeSceneIndex && _uiState.value.isAmbientEnabled) {
            val s = scenes[nextSceneIdx]
            audioEngine.startAmbient(s.ambientPreset, s.customMusicPath)
        }
        if (wasAutoPlaying) {
            playBeatAt(nextSceneIdx, nextBeatIdx, continueAutoPlay = true)
        }
    }

    fun previousBeat() {
        val state = _uiState.value
        val scenes = state.project.scenes
        val wasAutoPlaying = state.isCinemaAutoPlaying
        val prevSceneIdx: Int
        val prevBeatIdx: Int
        if (state.activeBeatIndex > 0) {
            prevSceneIdx = state.activeSceneIndex
            prevBeatIdx = state.activeBeatIndex - 1
        } else if (state.activeSceneIndex > 0) {
            prevSceneIdx = state.activeSceneIndex - 1
            prevBeatIdx = (scenes[prevSceneIdx].beats.lastIndex).coerceAtLeast(0)
        } else {
            return
        }

        stopCurrentBeatPlaybackOnly()
        _uiState.update { it.copy(activeSceneIndex = prevSceneIdx, activeBeatIndex = prevBeatIdx) }
        if (prevSceneIdx != state.activeSceneIndex && _uiState.value.isAmbientEnabled) {
            val s = scenes[prevSceneIdx]
            audioEngine.startAmbient(s.ambientPreset, s.customMusicPath)
        }
        if (wasAutoPlaying) {
            playBeatAt(prevSceneIdx, prevBeatIdx, continueAutoPlay = true)
        }
    }

    private fun stopCurrentBeatPlaybackOnly() {
        playbackJob?.cancel()
        playbackJob = null
        audioEngine.stopVoice()
        _uiState.update { it.copy(isSynthesizingVoice = false, isVoicePlaying = false) }
    }

    /**
     * Strict Character Voice Lock: computes deterministic cache key from beat + locked character voice settings.
     * Updating any character setting immediately invalidates stale keys for that character across the entire novel.
     */
    fun computeBeatCacheKey(beat: DialogueBeat): String {
        val state = _uiState.value
        val character = state.project.characterById(beat.characterId)
        val settings = state.voiceSettings
        val lang = state.language
        val spoken = beat.spokenText(
            language = lang,
            includeElevenTag = settings.selectedEngine == VoiceEngineType.ELEVENLABS_V4
        )
        return when (settings.selectedEngine) {
            VoiceEngineType.ELEVENLABS_V4 -> {
                "el|${settings.elevenLabsModel.modelId}|${character.id}|${character.elevenVoiceId}|" +
                    "${character.elevenStability}|${character.elevenSimilarityBoost}|${character.elevenStyle}|" +
                    "${lang.code}|${beat.id}|${spoken.hashCode()}"
            }
            VoiceEngineType.GEMINI_FLASH_TTS -> {
                "gm|${character.id}|${character.geminiVoiceName}|${character.displayActingStyle(lang).hashCode()}|" +
                    "${lang.code}|${beat.id}|${spoken.hashCode()}"
            }
        }
    }

    private fun playBeatAt(sceneIdx: Int, beatIdx: Int, continueAutoPlay: Boolean) {
        val state = _uiState.value
        val scene = state.project.scenes.getOrNull(sceneIdx) ?: return
        val beat = scene.beats.getOrNull(beatIdx) ?: return
        val character = state.project.characterById(beat.characterId)
        val settings = state.voiceSettings

        if (settings.selectedEngine == VoiceEngineType.ELEVENLABS_V4 &&
            settings.elevenLabsApiKey.isBlank()
        ) {
            _uiState.update {
                it.copy(
                    isCinemaAutoPlaying = false,
                    isSynthesizingVoice = false,
                    showMissingKeyDialog = true
                )
            }
            return
        }

        val cacheKey = computeBeatCacheKey(beat)
        stopCurrentBeatPlaybackOnly()

        playbackJob = viewModelScope.launch {
            val cachedClip = audioEngine.cache.get(cacheKey)
            val clip: CachedAudioClip = if (cachedClip != null) {
                cachedClip
            } else {
                _uiState.update { it.copy(isSynthesizingVoice = true) }
                val synthResult = synthesizeForCharacter(
                    character = character,
                    textForEleven = beat.spokenText(state.language, includeElevenTag = true),
                    textForGemini = beat.spokenText(state.language, includeElevenTag = false)
                )
                _uiState.update { it.copy(isSynthesizingVoice = false) }
                val generated = synthResult.getOrElse { err ->
                    _uiState.update { it.copy(isCinemaAutoPlaying = false) }
                    showBanner(err.localizedMessage ?: "Voice synthesis failed.", isError = true)
                    return@launch
                }
                audioEngine.cache.put(cacheKey, generated)
                generated
            }

            audioEngine.playVoiceClip(
                clip = clip,
                onCompleted = {
                    if (continueAutoPlay && _uiState.value.isCinemaAutoPlaying) {
                        playbackJob = viewModelScope.launch {
                            delay(420L)
                            if (_uiState.value.isCinemaAutoPlaying) {
                                nextBeat()
                            }
                        }
                    }
                },
                onError = { errMsg ->
                    _uiState.update { it.copy(isCinemaAutoPlaying = false) }
                    showBanner(errMsg, isError = true)
                }
            )
        }
    }

    /**
     * Tests or previews a specific character's locked voice in Cast Manager or Character Codex.
     */
    fun testCharacterVoice(characterId: String) {
        val state = _uiState.value
        if (state.testingCharacterId == characterId) {
            previewJob?.cancel()
            audioEngine.stopVoice()
            _uiState.update { it.copy(testingCharacterId = null) }
            return
        }

        if (state.voiceSettings.selectedEngine == VoiceEngineType.ELEVENLABS_V4 &&
            state.voiceSettings.elevenLabsApiKey.isBlank()
        ) {
            _uiState.update { it.copy(showMissingKeyDialog = true) }
            return
        }

        val character = state.project.characterById(characterId)
        pauseCinemaPlayback()
        previewJob?.cancel()

        val sampleEn = if (character.isNarrator) {
            "[softly] Midnight rain fell softly over Alexandria, awakening secrets written in gold."
        } else {
            "[whispers] Listen closely—${character.nameEn} remembers every word spoken in the dark."
        }
        val sampleAr = if (character.isNarrator) {
            "[softly] هطل مطر منتصف الليل بهدوء فوق الإسكندرية، فأيقظ أسراراً كُتبت بماء الذهب."
        } else {
            "[whispers] أصغِ جيداً... ${character.displayName(AppLanguage.AR)} يتذكر كل كلمة قيلت في الظلام."
        }

        val elevenLine = if (state.language == AppLanguage.AR) sampleAr else sampleEn
        val geminiLine = elevenLine.replace(Regex("^\\[[^\\]]+]\\s*"), "")

        previewJob = viewModelScope.launch {
            _uiState.update { it.copy(testingCharacterId = characterId) }
            val result = synthesizeForCharacter(character, elevenLine, geminiLine)
            result.fold(
                onSuccess = { clip ->
                    audioEngine.playVoiceClip(
                        clip = clip,
                        onCompleted = {
                            _uiState.update { it.copy(testingCharacterId = null) }
                        },
                        onError = { err ->
                            _uiState.update { it.copy(testingCharacterId = null) }
                            showBanner(err, isError = true)
                        }
                    )
                },
                onFailure = { err ->
                    _uiState.update { it.copy(testingCharacterId = null) }
                    showBanner(err.localizedMessage ?: "Voice preview failed.", isError = true)
                }
            )
        }
    }

    private suspend fun synthesizeForCharacter(
        character: CharacterProfile,
        textForEleven: String,
        textForGemini: String
    ): Result<CachedAudioClip> {
        val settings = _uiState.value.voiceSettings
        val lang = _uiState.value.language
        return when (settings.selectedEngine) {
            VoiceEngineType.ELEVENLABS_V4 -> {
                elevenLabsService.synthesizeSpeech(
                    apiKey = settings.elevenLabsApiKey,
                    model = settings.elevenLabsModel,
                    character = character,
                    text = textForEleven
                ).map { bytes -> CachedAudioClip(bytes = bytes, extension = "mp3") }
            }
            VoiceEngineType.GEMINI_FLASH_TTS -> {
                geminiService.synthesizeGeminiTts(
                    character = character,
                    dialogueText = textForGemini,
                    language = lang
                ).map { payload -> CachedAudioClip(bytes = payload.bytes, extension = payload.fileExtension) }
            }
        }
    }

    // --- Character Casting & Strict Voice Lock Mutations ---

    fun updateCharacter(characterId: String, transform: (CharacterProfile) -> CharacterProfile) {
        mutateProject { project ->
            project.copy(
                characters = project.characters.map { c ->
                    if (c.id == characterId) transform(c) else c
                }
            )
        }
    }

    fun addNewCharacter() {
        val count = _uiState.value.project.characters.size
        val id = "char_${System.currentTimeMillis()}"
        val newChar = CharacterProfile(
            id = id,
            isNarrator = false,
            nameEn = "Character $count",
            nameAr = "شخصية $count",
            roleEn = "Cast Member",
            roleAr = "عضو الطاقم",
            bioEn = "A newly cast voice in your cinematic novel.",
            bioAr = "شخصية جديدة في روايتك السينمائية.",
            elevenVoiceId = "pNInz6obpgDQGcFmaJgB",
            geminiVoiceName = "Kore",
            geminiActingStyleEn = "Expressive dramatic theatrical delivery",
            geminiActingStyleAr = "أداء مسرحي درامي معبر",
            accentColorHex = 0xFF8C72C9
        )
        mutateProject { project ->
            project.copy(characters = project.characters + newChar)
        }
    }

    // --- Director's Studio Actions ---

    fun updateDirectorRawProse(text: String) {
        _uiState.update { it.copy(directorRawProse = text) }
    }

    fun insertSampleProse() {
        val sample = if (_uiState.value.language == AppLanguage.AR) {
            DemoNovelSeed.SAMPLE_PASTE_AR
        } else {
            DemoNovelSeed.SAMPLE_PASTE_EN
        }
        _uiState.update { it.copy(directorRawProse = sample) }
    }

    fun directSceneWithGemini() {
        val prose = _uiState.value.directorRawProse.trim()
        if (prose.isBlank()) {
            showBanner(
                if (_uiState.value.language == AppLanguage.AR)
                    "يرجى لصق نص الرواية أولاً."
                else
                    "Please paste raw novel prose first.",
                isError = true
            )
            return
        }
        if (_uiState.value.isDirectingScene) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDirectingScene = true) }
            try {
                val result = geminiService.directSceneFromProse(
                    rawProse = prose,
                    existingCharacters = _uiState.value.project.characters
                )
                result.fold(
                    onSuccess = { directed ->
                        val updatedChars = _uiState.value.project.characters + directed.newCharacters
                        val updatedScenes = _uiState.value.project.scenes + directed.scene
                        val newProject = _uiState.value.project.copy(
                            characters = updatedChars,
                            scenes = updatedScenes
                        )
                        storage.saveProject(newProject)
                        _uiState.update {
                            it.copy(
                                project = newProject,
                                activeSceneIndex = updatedScenes.lastIndex,
                                activeBeatIndex = 0,
                                isDirectingScene = false,
                                directorRawProse = ""
                            )
                        }
                        showBanner(
                            if (_uiState.value.language == AppLanguage.AR)
                                "تم إخراج المشهد بنجاح عبر Gemini 3.8 Flash (${directed.scene.beats.size} لقطات)."
                            else
                                "Scene directed by Gemini 3.8 Flash (${directed.scene.beats.size} beats, ${directed.newCharacters.size} new cast).",
                            isError = false
                        )
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(isDirectingScene = false) }
                        showBanner(err.localizedMessage ?: "Direction failed.", isError = true)
                    }
                )
            } catch (e: CancellationException) {
                _uiState.update { it.copy(isDirectingScene = false) }
                throw e
            }
        }
    }

    fun generateSceneArtwork(sceneId: String) {
        val scene = _uiState.value.project.scenes.find { it.id == sceneId } ?: return
        val prompt = scene.visualPrompt.ifBlank { scene.titleEn }
        if (_uiState.value.generatingArtworkTargetId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(generatingArtworkTargetId = sceneId) }
            try {
                val result = geminiService.generateArtwork(prompt = prompt, isPortrait = false)
                result.fold(
                    onSuccess = { bitmap ->
                        val path = withContext(Dispatchers.IO) {
                            storage.saveBitmapAsset("scene_$sceneId", bitmap)
                        }
                        updateScene(sceneId) { it.copy(customArtworkPath = path) }
                        _uiState.update { it.copy(generatingArtworkTargetId = null) }
                        showBanner(
                            if (_uiState.value.language == AppLanguage.AR)
                                "تم توليد اللوحة السينمائية عبر gemini-nano-banana-2.1."
                            else
                                "Scene artwork rendered via gemini-nano-banana-2.1.",
                            isError = false
                        )
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(generatingArtworkTargetId = null) }
                        showBanner(err.localizedMessage ?: "Artwork generation failed.", isError = true)
                    }
                )
            } catch (e: CancellationException) {
                _uiState.update { it.copy(generatingArtworkTargetId = null) }
                throw e
            }
        }
    }

    fun generateCharacterPortrait(characterId: String) {
        val char = _uiState.value.project.characters.find { it.id == characterId } ?: return
        if (_uiState.value.generatingArtworkTargetId != null) return

        val prompt = "${char.nameEn}, ${char.roleEn}. ${char.bioEn}"
        viewModelScope.launch {
            _uiState.update { it.copy(generatingArtworkTargetId = characterId) }
            try {
                val result = geminiService.generateArtwork(prompt = prompt, isPortrait = true)
                result.fold(
                    onSuccess = { bitmap ->
                        val path = withContext(Dispatchers.IO) {
                            storage.saveBitmapAsset("char_$characterId", bitmap)
                        }
                        updateCharacter(characterId) { it.copy(customImagePath = path) }
                        _uiState.update { it.copy(generatingArtworkTargetId = null) }
                        showBanner(
                            if (_uiState.value.language == AppLanguage.AR)
                                "تم توليد بورتريه الشخصية عبر gemini-nano-banana-2.1."
                            else
                                "Character portrait rendered via gemini-nano-banana-2.1.",
                            isError = false
                        )
                    },
                    onFailure = { err ->
                        _uiState.update { it.copy(generatingArtworkTargetId = null) }
                        showBanner(err.localizedMessage ?: "Portrait generation failed.", isError = true)
                    }
                )
            } catch (e: CancellationException) {
                _uiState.update { it.copy(generatingArtworkTargetId = null) }
                throw e
            }
        }
    }

    fun generateSceneSfx(sceneId: String) {
        val state = _uiState.value
        val scene = state.project.scenes.find { it.id == sceneId } ?: return
        if (state.voiceSettings.elevenLabsApiKey.isBlank()) {
            _uiState.update { it.copy(showMissingKeyDialog = true) }
            return
        }
        if (state.generatingSfxSceneId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(generatingSfxSceneId = sceneId) }
            val result = elevenLabsService.generateSoundEffect(
                apiKey = state.voiceSettings.elevenLabsApiKey,
                prompt = scene.sfxPrompt.ifBlank { scene.titleEn }
            )
            result.fold(
                onSuccess = { audioBytes ->
                    val savedPath = withContext(Dispatchers.IO) {
                        storage.saveGeneratedSfx(sceneId, audioBytes)
                    }
                    updateScene(sceneId) { it.copy(sfxFilePath = savedPath) }
                    _uiState.update { it.copy(generatingSfxSceneId = null) }
                    audioEngine.playSfxFile(savedPath)
                    showBanner(
                        if (_uiState.value.language == AppLanguage.AR)
                            "تم توليد المؤثر الصوتي عبر eleven_text_to_sound_v3."
                        else
                            "SFX synthesized via eleven_text_to_sound_v3.",
                        isError = false
                    )
                },
                onFailure = { err ->
                    _uiState.update { it.copy(generatingSfxSceneId = null) }
                    showBanner(err.localizedMessage ?: "SFX generation failed.", isError = true)
                }
            )
        }
    }

    fun attachPickedSceneImage(sceneId: String, uri: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { storage.copyPickedImage("scene_$sceneId", uri) }
            if (saved != null) {
                updateScene(sceneId) { it.copy(customArtworkPath = saved) }
            }
        }
    }

    fun attachPickedCharacterImage(characterId: String, uri: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { storage.copyPickedImage("char_$characterId", uri) }
            if (saved != null) {
                updateCharacter(characterId) { it.copy(customImagePath = saved) }
            }
        }
    }

    fun attachPickedSceneMusic(sceneId: String, uri: Uri) {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { storage.copyPickedAudio("music_$sceneId", uri) }
            if (saved != null) {
                val (path, displayName) = saved
                updateScene(sceneId) { it.copy(customMusicPath = path, customMusicName = displayName) }
                if (_uiState.value.activeScene.id == sceneId && _uiState.value.isAmbientEnabled) {
                    audioEngine.startAmbient(_uiState.value.activeScene.ambientPreset, path)
                }
                showBanner(
                    if (_uiState.value.language == AppLanguage.AR)
                        "تم إرفاق المقطوعة الصوتية: $displayName"
                    else
                        "Attached custom soundtrack: $displayName",
                    isError = false
                )
            }
        }
    }

    fun clearCustomSceneMusic(sceneId: String) {
        updateScene(sceneId) { it.copy(customMusicPath = null, customMusicName = null) }
        if (_uiState.value.activeScene.id == sceneId && _uiState.value.isAmbientEnabled) {
            audioEngine.startAmbient(_uiState.value.activeScene.ambientPreset, null)
        }
    }

    fun updateScene(sceneId: String, transform: (NovelScene) -> NovelScene) {
        mutateProject { project ->
            project.copy(
                scenes = project.scenes.map { s -> if (s.id == sceneId) transform(s) else s }
            )
        }
    }

    fun updateBeat(sceneId: String, beatId: String, transform: (DialogueBeat) -> DialogueBeat) {
        updateScene(sceneId) { scene ->
            scene.copy(
                beats = scene.beats.map { b -> if (b.id == beatId) transform(b) else b }
            )
        }
    }

    fun addBeatToScene(sceneId: String) {
        val defaultChar = _uiState.value.project.characters.first()
        val newBeat = DialogueBeat(
            id = "beat_${System.currentTimeMillis()}",
            characterId = defaultChar.id,
            isNarration = defaultChar.isNarrator,
            textEn = "New cinematic line...",
            textAr = "جملة سينمائية جديدة...",
            expressionTag = ""
        )
        updateScene(sceneId) { it.copy(beats = it.beats + newBeat) }
    }

    fun deleteBeatFromScene(sceneId: String, beatId: String) {
        updateScene(sceneId) { scene ->
            if (scene.beats.size <= 1) scene
            else scene.copy(beats = scene.beats.filterNot { it.id == beatId })
        }
    }

    // --- Project Persistence: Export / Import JSON & Reset ---

    fun exportProjectToUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = storage.serializeProjectJson(_uiState.value.project)
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Cannot open destination file.")
                }
                showBanner(
                    if (_uiState.value.language == AppLanguage.AR)
                        "تم تصدير مشروع الرواية بصيغة JSON بنجاح."
                    else
                        "Novel project exported to .json successfully.",
                    isError = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                showBanner("Export failed: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun importProjectFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val raw = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { inp ->
                        inp.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Cannot read selected JSON file.")
                }
                val parsed = storage.parseProjectJson(raw)
                pauseCinemaPlayback()
                audioEngine.stopAmbient()
                audioEngine.cache.clear()
                storage.saveProject(parsed)
                _uiState.update {
                    it.copy(
                        project = parsed,
                        activeSceneIndex = 0,
                        activeBeatIndex = 0,
                        isAmbientEnabled = false
                    )
                }
                showBanner(
                    if (_uiState.value.language == AppLanguage.AR)
                        "تم استيراد مشروع الرواية بنجاح."
                    else
                        "Novel project imported successfully.",
                    isError = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                showBanner("Invalid project JSON: ${e.localizedMessage}", isError = true)
            }
        }
    }

    fun resetToDemoChapter() {
        pauseCinemaPlayback()
        audioEngine.stopAmbient()
        audioEngine.cache.clear()
        val demo = DemoNovelSeed.createDemoProject()
        storage.saveProject(demo)
        _uiState.update {
            it.copy(
                project = demo,
                activeSceneIndex = 0,
                activeBeatIndex = 0,
                isAmbientEnabled = false
            )
        }
        showBanner(
            if (_uiState.value.language == AppLanguage.AR)
                "تمت استعادة الفصل التجريبي الأصلي."
            else
                "Demo chapter restored.",
            isError = false
        )
    }

    private fun mutateProject(transform: (NovelProject) -> NovelProject) {
        val updated = transform(_uiState.value.project)
        storage.saveProject(updated)
        _uiState.update { state ->
            val safeSceneIdx = state.activeSceneIndex.coerceIn(0, (updated.scenes.lastIndex).coerceAtLeast(0))
            val safeBeatIdx = state.activeBeatIndex.coerceIn(
                0,
                (updated.scenes[safeSceneIdx].beats.lastIndex).coerceAtLeast(0)
            )
            state.copy(
                project = updated,
                activeSceneIndex = safeSceneIdx,
                activeBeatIndex = safeBeatIdx
            )
        }
    }

    private fun showBanner(message: String, isError: Boolean) {
        _uiState.update {
            it.copy(statusBannerMessage = message, statusBannerIsError = isError)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
