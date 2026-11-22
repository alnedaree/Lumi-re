package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class ProjectStorageRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val projectFile: File get() = File(context.filesDir, PROJECT_FILENAME)
    private val artworkDir: File get() = File(context.filesDir, "artwork").apply { mkdirs() }
    private val audioDir: File get() = File(context.filesDir, "audio").apply { mkdirs() }

    fun loadProject(): NovelProject {
        return try {
            if (projectFile.exists()) {
                val json = projectFile.readText(Charsets.UTF_8)
                parseProjectJson(json)
            } else {
                DemoNovelSeed.createDemoProject().also { saveProject(it) }
            }
        } catch (_: Exception) {
            DemoNovelSeed.createDemoProject()
        }
    }

    fun saveProject(project: NovelProject) {
        try {
            projectFile.writeText(serializeProjectJson(project), Charsets.UTF_8)
        } catch (_: Exception) {
            // Non-fatal persistence error
        }
    }

    fun loadLanguage(): AppLanguage {
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.EN.code)
        return AppLanguage.entries.find { it.code == code } ?: AppLanguage.EN
    }

    fun saveLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }

    fun loadThemeMode(): LumiereThemeMode {
        val name = prefs.getString(KEY_THEME, LumiereThemeMode.OBSIDIAN_CINEMA.name)
        return LumiereThemeMode.entries.find { it.name == name } ?: LumiereThemeMode.OBSIDIAN_CINEMA
    }

    fun saveThemeMode(themeMode: LumiereThemeMode) {
        prefs.edit().putString(KEY_THEME, themeMode.name).apply()
    }

    fun loadVoiceSettings(): VoiceEngineSettings {
        val engineName = prefs.getString(KEY_VOICE_ENGINE, VoiceEngineType.ELEVENLABS_V4.name)
        val engine = VoiceEngineType.entries.find { it.name == engineName }
            ?: VoiceEngineType.ELEVENLABS_V4
        val apiKey = prefs.getString(KEY_ELEVEN_API_KEY, "") ?: ""
        val modelId = prefs.getString(KEY_ELEVEN_MODEL, ElevenLabsModel.ELEVEN_V4.modelId)
        val model = ElevenLabsModel.entries.find { it.modelId == modelId }
            ?: ElevenLabsModel.ELEVEN_V4
        return VoiceEngineSettings(
            selectedEngine = engine,
            elevenLabsApiKey = apiKey,
            elevenLabsModel = model
        )
    }

    fun saveVoiceSettings(settings: VoiceEngineSettings) {
        prefs.edit()
            .putString(KEY_VOICE_ENGINE, settings.selectedEngine.name)
            .putString(KEY_ELEVEN_API_KEY, settings.elevenLabsApiKey)
            .putString(KEY_ELEVEN_MODEL, settings.elevenLabsModel.modelId)
            .apply()
    }

    fun loadMixerState(): MixerState {
        return MixerState(
            voiceVolume = prefs.getFloat(KEY_VOL_VOICE, 0.95f),
            musicVolume = prefs.getFloat(KEY_VOL_MUSIC, 0.35f),
            sfxVolume = prefs.getFloat(KEY_VOL_SFX, 0.72f)
        )
    }

    fun saveMixerState(mixer: MixerState) {
        prefs.edit()
            .putFloat(KEY_VOL_VOICE, mixer.voiceVolume)
            .putFloat(KEY_VOL_MUSIC, mixer.musicVolume)
            .putFloat(KEY_VOL_SFX, mixer.sfxVolume)
            .apply()
    }

    fun saveBitmapAsset(prefix: String, bitmap: Bitmap): String {
        val file = File(artworkDir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
        }
        return file.absolutePath
    }

    fun copyPickedImage(prefix: String, uri: Uri): String? {
        return try {
            val file = File(artworkDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    fun copyPickedAudio(prefix: String, uri: Uri): Pair<String, String>? {
        return try {
            val displayName = resolveDisplayName(uri) ?: "custom_track.mp3"
            val ext = displayName.substringAfterLast('.', "mp3").lowercase()
            val file = File(audioDir, "${prefix}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            file.absolutePath to displayName
        } catch (_: Exception) {
            null
        }
    }

    fun saveGeneratedSfx(sceneId: String, audioBytes: ByteArray): String {
        val safeId = sceneId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val file = File(audioDir, "sfx_${safeId}_${System.currentTimeMillis()}.mp3")
        file.writeBytes(audioBytes)
        return file.absolutePath
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    fun serializeProjectJson(project: NovelProject): String {
        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("id", project.id)
        root.put("titleEn", project.titleEn)
        root.put("titleAr", project.titleAr)
        root.put("authorEn", project.authorEn)
        root.put("authorAr", project.authorAr)

        val charsArray = JSONArray()
        project.characters.forEach { c ->
            charsArray.put(JSONObject().apply {
                put("id", c.id)
                put("isNarrator", c.isNarrator)
                put("nameEn", c.nameEn)
                put("nameAr", c.nameAr)
                put("roleEn", c.roleEn)
                put("roleAr", c.roleAr)
                put("bioEn", c.bioEn)
                put("bioAr", c.bioAr)
                put("bundledPortraitKey", c.bundledPortraitKey ?: JSONObject.NULL)
                put("customImagePath", c.customImagePath ?: JSONObject.NULL)
                put("elevenVoiceId", c.elevenVoiceId)
                put("elevenStability", c.elevenStability.toDouble())
                put("elevenSimilarityBoost", c.elevenSimilarityBoost.toDouble())
                put("elevenStyle", c.elevenStyle.toDouble())
                put("geminiVoiceName", c.geminiVoiceName)
                put("geminiActingStyleEn", c.geminiActingStyleEn)
                put("geminiActingStyleAr", c.geminiActingStyleAr)
                put("accentColorHex", c.accentColorHex)
            })
        }
        root.put("characters", charsArray)

        val scenesArray = JSONArray()
        project.scenes.forEach { s ->
            val beatsArray = JSONArray()
            s.beats.forEach { b ->
                beatsArray.put(JSONObject().apply {
                    put("id", b.id)
                    put("characterId", b.characterId)
                    put("isNarration", b.isNarration)
                    put("textEn", b.textEn)
                    put("textAr", b.textAr)
                    put("expressionTag", b.expressionTag)
                })
            }
            scenesArray.put(JSONObject().apply {
                put("id", s.id)
                put("titleEn", s.titleEn)
                put("titleAr", s.titleAr)
                put("subtitleEn", s.subtitleEn)
                put("subtitleAr", s.subtitleAr)
                put("visualPrompt", s.visualPrompt)
                put("bundledArtworkKey", s.bundledArtworkKey ?: JSONObject.NULL)
                put("customArtworkPath", s.customArtworkPath ?: JSONObject.NULL)
                put("ambientPreset", s.ambientPreset.name)
                put("customMusicPath", s.customMusicPath ?: JSONObject.NULL)
                put("customMusicName", s.customMusicName ?: JSONObject.NULL)
                put("sfxPrompt", s.sfxPrompt)
                put("sfxFilePath", s.sfxFilePath ?: JSONObject.NULL)
                put("beats", beatsArray)
            })
        }
        root.put("scenes", scenesArray)

        return root.toString(2)
    }

    fun parseProjectJson(rawJson: String): NovelProject {
        val root = JSONObject(rawJson)
        val charsJson = root.getJSONArray("characters")
        val characters = (0 until charsJson.length()).map { i ->
            val o = charsJson.getJSONObject(i)
            CharacterProfile(
                id = o.getString("id"),
                isNarrator = o.optBoolean("isNarrator", false),
                nameEn = o.optString("nameEn", "Character"),
                nameAr = o.optString("nameAr", ""),
                roleEn = o.optString("roleEn", ""),
                roleAr = o.optString("roleAr", ""),
                bioEn = o.optString("bioEn", ""),
                bioAr = o.optString("bioAr", ""),
                bundledPortraitKey = o.optStringOrNull("bundledPortraitKey"),
                customImagePath = o.optStringOrNull("customImagePath"),
                elevenVoiceId = o.optString("elevenVoiceId", "JBFqnCBsd6RMkjVDRZzb"),
                elevenStability = o.optDouble("elevenStability", 0.45).toFloat().coerceIn(0f, 1f),
                elevenSimilarityBoost = o.optDouble("elevenSimilarityBoost", 0.80).toFloat().coerceIn(0f, 1f),
                elevenStyle = o.optDouble("elevenStyle", 0.35).toFloat().coerceIn(0f, 1f),
                geminiVoiceName = o.optString("geminiVoiceName", "Charon"),
                geminiActingStyleEn = o.optString("geminiActingStyleEn", ""),
                geminiActingStyleAr = o.optString("geminiActingStyleAr", ""),
                accentColorHex = o.optLong("accentColorHex", 0xFFD4AF37)
            )
        }
        require(characters.isNotEmpty()) { "Project JSON must contain at least one character." }

        val scenesJson = root.getJSONArray("scenes")
        val scenes = (0 until scenesJson.length()).map { i ->
            val o = scenesJson.getJSONObject(i)
            val beatsJson = o.getJSONArray("beats")
            val beats = (0 until beatsJson.length()).map { j ->
                val b = beatsJson.getJSONObject(j)
                DialogueBeat(
                    id = b.optString("id", "beat_${i}_$j"),
                    characterId = b.optString("characterId", characters.first().id),
                    isNarration = b.optBoolean("isNarration", false),
                    textEn = b.optString("textEn", ""),
                    textAr = b.optString("textAr", ""),
                    expressionTag = b.optString("expressionTag", "")
                )
            }
            val presetName = o.optString("ambientPreset", AmbientPreset.RAIN.name)
            NovelScene(
                id = o.optString("id", "scene_$i"),
                titleEn = o.optString("titleEn", "Untitled Scene"),
                titleAr = o.optString("titleAr", ""),
                subtitleEn = o.optString("subtitleEn", ""),
                subtitleAr = o.optString("subtitleAr", ""),
                visualPrompt = o.optString("visualPrompt", ""),
                bundledArtworkKey = o.optStringOrNull("bundledArtworkKey"),
                customArtworkPath = o.optStringOrNull("customArtworkPath"),
                ambientPreset = AmbientPreset.entries.find { it.name == presetName }
                    ?: AmbientPreset.RAIN,
                customMusicPath = o.optStringOrNull("customMusicPath"),
                customMusicName = o.optStringOrNull("customMusicName"),
                sfxPrompt = o.optString("sfxPrompt", ""),
                sfxFilePath = o.optStringOrNull("sfxFilePath"),
                beats = beats
            )
        }
        require(scenes.isNotEmpty()) { "Project JSON must contain at least one scene." }

        return NovelProject(
            id = root.optString("id", "imported_${System.currentTimeMillis()}"),
            titleEn = root.optString("titleEn", "Imported Novel"),
            titleAr = root.optString("titleAr", ""),
            authorEn = root.optString("authorEn", "Lumière Studio"),
            authorAr = root.optString("authorAr", ""),
            characters = characters,
            scenes = scenes
        )
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key, "")
        return value.ifBlank { null }
    }

    companion object {
        private const val PREFS_NAME = "lumiere_studio_prefs"
        private const val PROJECT_FILENAME = "lumiere_project.json"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_VOICE_ENGINE = "voice_engine"
        private const val KEY_ELEVEN_API_KEY = "elevenlabs_api_key"
        private const val KEY_ELEVEN_MODEL = "elevenlabs_model_id"
        private const val KEY_VOL_VOICE = "vol_voice"
        private const val KEY_VOL_MUSIC = "vol_music"
        private const val KEY_VOL_SFX = "vol_sfx"
    }
}
