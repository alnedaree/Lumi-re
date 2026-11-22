package com.example.model

enum class AppLanguage(val code: String) {
    EN("en"),
    AR("ar")
}

enum class LumiereThemeMode {
    OBSIDIAN_CINEMA,
    ROYAL_PARCHMENT,
    DAYLIGHT_EDITORIAL
}

enum class WorkspaceMode {
    CINEMA_READER,
    DIRECTORS_STUDIO
}

enum class VoiceEngineType {
    ELEVENLABS_V4,
    GEMINI_FLASH_TTS
}

/**
 * Strict Technical Lock: Allowed ElevenLabs model_id values ONLY.
 */
enum class ElevenLabsModel(val modelId: String) {
    ELEVEN_V4("eleven_v4"),
    ELEVEN_V4_TURBO("eleven_v4_turbo")
}

enum class AmbientPreset {
    RAIN,
    NOIR_SUSPENSE,
    NOSTALGIC_PIANO,
    NIGHT_WIND,
    NONE
}

object ModelIds {
    const val ELEVENLABS_DEFAULT = "eleven_v4"
    const val ELEVENLABS_TURBO = "eleven_v4_turbo"
    const val ELEVENLABS_SFX = "eleven_text_to_sound_v3"
    const val GEMINI_TTS = "gemini-3.8-flash-tts"
    const val GEMINI_TTS_API_VERSION = "v1beta"
    const val GEMINI_DIRECTOR = "gemini-3.8-flash"
    const val GEMINI_IMAGE = "gemini-nano-banana-2.1"
}

val GEMINI_PREBUILT_VOICES = listOf(
    "Charon",
    "Aoede",
    "Fenrir",
    "Kore",
    "Puck",
    "Zephyr",
    "Orus",
    "Leda"
)

val ELEVEN_EXPRESSIVE_TAGS = listOf(
    "[whispers]",
    "[sighs]",
    "[laughs]",
    "[pauses]",
    "[softly]",
    "[dramatically]"
)

data class CharacterProfile(
    val id: String,
    val isNarrator: Boolean = false,
    val nameEn: String,
    val nameAr: String,
    val roleEn: String,
    val roleAr: String,
    val bioEn: String,
    val bioAr: String,
    val bundledPortraitKey: String? = null,
    val customImagePath: String? = null,
    // Engine A: ElevenLabs v4 voice settings
    val elevenVoiceId: String = "JBFqnCBsd6RMkjVDRZzb",
    val elevenStability: Float = 0.45f,
    val elevenSimilarityBoost: Float = 0.80f,
    val elevenStyle: Float = 0.35f,
    // Engine B: Google Gemini 3.8 Flash TTS settings
    val geminiVoiceName: String = "Charon",
    val geminiActingStyleEn: String = "Calm cinematic narrator, warm hushed gravitas",
    val geminiActingStyleAr: String = "راوٍ سينمائي هادئ بنبرة عميقة ودافئة",
    val accentColorHex: Long = 0xFFD4AF37
) {
    fun displayName(language: AppLanguage): String =
        if (language == AppLanguage.AR && nameAr.isNotBlank()) nameAr else nameEn.ifBlank { nameAr }

    fun displayRole(language: AppLanguage): String =
        if (language == AppLanguage.AR && roleAr.isNotBlank()) roleAr else roleEn.ifBlank { roleAr }

    fun displayBio(language: AppLanguage): String =
        if (language == AppLanguage.AR && bioAr.isNotBlank()) bioAr else bioEn.ifBlank { bioAr }

    fun displayActingStyle(language: AppLanguage): String =
        if (language == AppLanguage.AR && geminiActingStyleAr.isNotBlank()) {
            geminiActingStyleAr
        } else {
            geminiActingStyleEn.ifBlank { geminiActingStyleAr }
        }
}

data class DialogueBeat(
    val id: String,
    val characterId: String,
    val isNarration: Boolean,
    val textEn: String,
    val textAr: String,
    val expressionTag: String = ""
) {
    fun displayText(language: AppLanguage): String =
        if (language == AppLanguage.AR && textAr.isNotBlank()) textAr else textEn.ifBlank { textAr }

    fun spokenText(language: AppLanguage, includeElevenTag: Boolean): String {
        val base = displayText(language).trim()
        return if (includeElevenTag && expressionTag.isNotBlank() && !base.startsWith("[")) {
            "${expressionTag.trim()} $base"
        } else {
            base
        }
    }
}

data class NovelScene(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val subtitleEn: String,
    val subtitleAr: String,
    val visualPrompt: String,
    val bundledArtworkKey: String? = null,
    val customArtworkPath: String? = null,
    val ambientPreset: AmbientPreset = AmbientPreset.RAIN,
    val customMusicPath: String? = null,
    val customMusicName: String? = null,
    val sfxPrompt: String = "",
    val sfxFilePath: String? = null,
    val beats: List<DialogueBeat>
) {
    fun displayTitle(language: AppLanguage): String =
        if (language == AppLanguage.AR && titleAr.isNotBlank()) titleAr else titleEn.ifBlank { titleAr }

    fun displaySubtitle(language: AppLanguage): String =
        if (language == AppLanguage.AR && subtitleAr.isNotBlank()) subtitleAr else subtitleEn.ifBlank { subtitleAr }
}

data class NovelProject(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val authorEn: String,
    val authorAr: String,
    val characters: List<CharacterProfile>,
    val scenes: List<NovelScene>
) {
    fun displayTitle(language: AppLanguage): String =
        if (language == AppLanguage.AR && titleAr.isNotBlank()) titleAr else titleEn.ifBlank { titleAr }

    fun displayAuthor(language: AppLanguage): String =
        if (language == AppLanguage.AR && authorAr.isNotBlank()) authorAr else authorEn.ifBlank { authorAr }

    fun characterById(id: String): CharacterProfile =
        characters.find { it.id == id }
            ?: characters.firstOrNull { it.isNarrator }
            ?: characters.first()
}

data class VoiceEngineSettings(
    val selectedEngine: VoiceEngineType = VoiceEngineType.ELEVENLABS_V4,
    val elevenLabsApiKey: String = "",
    val elevenLabsModel: ElevenLabsModel = ElevenLabsModel.ELEVEN_V4
)

data class MixerState(
    val voiceVolume: Float = 0.95f,
    val musicVolume: Float = 0.35f,
    val sfxVolume: Float = 0.72f
)
