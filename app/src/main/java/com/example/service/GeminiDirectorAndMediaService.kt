package com.example.service

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AmbientPreset
import com.example.model.AppLanguage
import com.example.model.CharacterProfile
import com.example.model.DialogueBeat
import com.example.model.GEMINI_PREBUILT_VOICES
import com.example.model.ModelIds
import com.example.model.NovelScene
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val TAG = "GeminiMediaService"

data class DirectedSceneResult(
    val scene: NovelScene,
    val newCharacters: List<CharacterProfile>
)

data class SynthesizedAudioPayload(
    val bytes: ByteArray,
    val fileExtension: String
)

class GeminiDirectorAndMediaService {

    private val directorSchema: Schema by lazy {
        Schema.obj(
            properties = mapOf(
                "sceneTitleEn" to Schema.string("Evocative cinematic scene title in English"),
                "sceneTitleAr" to Schema.string("Evocative cinematic scene title in Arabic"),
                "subtitleEn" to Schema.string("Short act/location subtitle in English"),
                "subtitleAr" to Schema.string("Short act/location subtitle in Arabic"),
                "visualPrompt" to Schema.string("Rich widescreen cinematic film still prompt describing lighting, mood, setting, and era for image generation"),
                "ambientPreset" to Schema.enumeration(
                    values = listOf("RAIN", "NOIR_SUSPENSE", "NOSTALGIC_PIANO", "NIGHT_WIND", "NONE"),
                    description = "Best matching background soundscape preset"
                ),
                "sfxPrompt" to Schema.string("Specific atmospheric sound effect prompt for ElevenLabs sound generation"),
                "detectedCharacters" to Schema.array(
                    description = "All speaking characters detected (excluding the Narrator)",
                    items = Schema.obj(
                        properties = mapOf(
                            "characterId" to Schema.string("Stable snake_case id like char_sebastian"),
                            "nameEn" to Schema.string("Character name in English"),
                            "nameAr" to Schema.string("Character name in Arabic"),
                            "roleEn" to Schema.string("Short role epithet in English"),
                            "roleAr" to Schema.string("Short role epithet in Arabic"),
                            "bioEn" to Schema.string("1-2 sentence atmospheric bio in English"),
                            "bioAr" to Schema.string("1-2 sentence atmospheric bio in Arabic"),
                            "suggestedGeminiVoice" to Schema.enumeration(
                                values = GEMINI_PREBUILT_VOICES,
                                description = "Suggested Gemini voice fitting the character"
                            ),
                            "actingStyleEn" to Schema.string("Voice acting direction in English"),
                            "actingStyleAr" to Schema.string("Voice acting direction in Arabic")
                        )
                    )
                ),
                "beats" to Schema.array(
                    description = "Ordered narration and dialogue beats split from the source prose",
                    items = Schema.obj(
                        properties = mapOf(
                            "characterId" to Schema.string("Use 'narrator' for narration or the matching characterId for dialogue"),
                            "isNarration" to Schema.boolean("True for narrator prose, false for spoken character dialogue"),
                            "textEn" to Schema.string("Beat text in English"),
                            "textAr" to Schema.string("Beat text in Arabic"),
                            "expressionTag" to Schema.string("Optional Eleven v4 expressive tag such as [whispers], [sighs], [laughs], [pauses], [softly], [dramatically], or empty string")
                        )
                    )
                )
            )
        )
    }

    private val directorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ModelIds.GEMINI_DIRECTOR,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = directorSchema
                temperature = 0.6f
            }
        )
    }

    private val sceneImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ModelIds.GEMINI_IMAGE,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.LANDSCAPE_16x9)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    private val portraitImageModel by lazy {
        Firebase.ai.generativeModel(
            modelName = ModelIds.GEMINI_IMAGE,
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
                imageConfig = ImageConfig.builder()
                    .setAspectRatio(AspectRatio.SQUARE_1x1)
                    .setImageSize(ImageSize.SIZE_1K)
                    .build()
            }
        )
    }

    /**
     * AI Novel Director powered by gemini-3.8-flash.
     * Splits raw Arabic or English novel prose into voice-locked beats, cast, visual prompts, and audio cues.
     */
    suspend fun directSceneFromProse(
        rawProse: String,
        existingCharacters: List<CharacterProfile>
    ): Result<DirectedSceneResult> {
        if (rawProse.isBlank()) {
            return Result.failure(IllegalArgumentException("Paste novel text first before directing."))
        }
        return try {
            val existingRoster = existingCharacters.joinToString("\n") { c ->
                "- id=\"${c.id}\", nameEn=\"${c.nameEn}\", nameAr=\"${c.nameAr}\", isNarrator=${c.isNarrator}"
            }
            val prompt = """
                You are an acclaimed cinema director and literary adapter for LUMIÈRE, a luxury interactive movie-book app.
                Analyze the following novel excerpt (which may be in Arabic, English, or mixed) and direct it into a structured cinematic scene.

                RULES:
                1. Preserve the exact narrative flow and split narration vs. character dialogue into clean sequential beats.
                2. Reuse existing character IDs below if the same character appears; otherwise create a concise snake_case id like "char_<name>". Always use "narrator" for narration beats.
                3. Provide BOTH faithful English (textEn / titleEn) AND literary Arabic (textAr / titleAr) for every beat, title, and character bio so the reader can switch RTL/LTR languages seamlessly.
                4. Assign subtle Eleven v4 expressive tags ([whispers], [sighs], [laughs], [pauses], [softly], [dramatically]) only where emotionally fitting.
                5. Craft an ultra-cinematic visualPrompt (in English) suitable for film still rendering, pick the best ambientPreset, and write a vivid sfxPrompt.

                EXISTING CAST ROSTER:
                $existingRoster

                RAW NOVEL EXCERPT:
                $rawProse
            """.trimIndent()

            val response = withTimeout(60_000L) {
                directorModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Gemini Director returned an empty response."))

            val json = JSONObject(jsonText)
            val existingIds = existingCharacters.map { it.id }.toSet()
            val defaultElevenVoices = listOf(
                "EXAVITQu4vr4xnSDxMaL",
                "N2lVS1w4EtoT3dr4eOWO",
                "pNInz6obpgDQGcFmaJgB",
                "XB0fDUnXU5powFXDhCwa",
                "onwK4e9ZLuTAKqWW03F9"
            )
            val paletteAccents = listOf(0xFF4CA890L, 0xFFC98A4BL, 0xFF8A72C9L, 0xFFD47A6AL, 0xFF5D92C9L)

            val newCharacters = mutableListOf<CharacterProfile>()
            val detectedArr = json.optJSONArray("detectedCharacters")
            if (detectedArr != null) {
                for (i in 0 until detectedArr.length()) {
                    val obj = detectedArr.getJSONObject(i)
                    val cid = obj.optString("characterId", "").trim()
                    if (cid.isNotBlank() && cid != "narrator" && cid !in existingIds && newCharacters.none { it.id == cid }) {
                        newCharacters.add(
                            CharacterProfile(
                                id = cid,
                                isNarrator = false,
                                nameEn = obj.optString("nameEn", cid),
                                nameAr = obj.optString("nameAr", obj.optString("nameEn", cid)),
                                roleEn = obj.optString("roleEn", "Supporting Cast"),
                                roleAr = obj.optString("roleAr", "شخصية في الرواية"),
                                bioEn = obj.optString("bioEn", ""),
                                bioAr = obj.optString("bioAr", ""),
                                elevenVoiceId = defaultElevenVoices[(existingCharacters.size + i) % defaultElevenVoices.size],
                                elevenStability = 0.45f,
                                elevenSimilarityBoost = 0.82f,
                                elevenStyle = 0.42f,
                                geminiVoiceName = obj.optString("suggestedGeminiVoice", "Aoede"),
                                geminiActingStyleEn = obj.optString("actingStyleEn", "Cinematic dramatic delivery"),
                                geminiActingStyleAr = obj.optString("actingStyleAr", "أداء درامي سينمائي معبر"),
                                accentColorHex = paletteAccents[(existingCharacters.size + i) % paletteAccents.size]
                            )
                        )
                    }
                }
            }

            val allKnownIds = existingIds + newCharacters.map { it.id }
            val beatsArr = json.optJSONArray("beats")
            val parsedBeats = mutableListOf<DialogueBeat>()
            if (beatsArr != null) {
                for (i in 0 until beatsArr.length()) {
                    val b = beatsArr.getJSONObject(i)
                    val rawCharId = b.optString("characterId", "narrator").trim()
                    val isNarr = b.optBoolean("isNarration", rawCharId == "narrator")
                    val resolvedCharId = if (isNarr || rawCharId !in allKnownIds) "narrator" else rawCharId
                    parsedBeats.add(
                        DialogueBeat(
                            id = "beat_${System.currentTimeMillis()}_$i",
                            characterId = resolvedCharId,
                            isNarration = isNarr,
                            textEn = b.optString("textEn", ""),
                            textAr = b.optString("textAr", ""),
                            expressionTag = b.optString("expressionTag", "")
                        )
                    )
                }
            }

            if (parsedBeats.isEmpty()) {
                return Result.failure(IllegalStateException("No dialogue beats could be parsed from the excerpt."))
            }

            val presetStr = json.optString("ambientPreset", AmbientPreset.NOIR_SUSPENSE.name)
            val ambient = AmbientPreset.entries.find { it.name == presetStr } ?: AmbientPreset.NOIR_SUSPENSE

            val newScene = NovelScene(
                id = "scene_${System.currentTimeMillis()}",
                titleEn = json.optString("sceneTitleEn", "Directed Scene"),
                titleAr = json.optString("sceneTitleAr", "مشهد مُخرج"),
                subtitleEn = json.optString("subtitleEn", "DIRECTED BY GEMINI 3.8 FLASH"),
                subtitleAr = json.optString("subtitleAr", "إخراج Gemini 3.8 Flash"),
                visualPrompt = json.optString("visualPrompt", "Cinematic moody chiaroscuro film still"),
                ambientPreset = ambient,
                sfxPrompt = json.optString("sfxPrompt", ""),
                beats = parsedBeats
            )

            Result.success(DirectedSceneResult(scene = newScene, newCharacters = newCharacters))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Director call timed out", e)
            Result.failure(Exception("Scene direction timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Director call failed", e)
            Result.failure(Exception("Gemini Director failed: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    /**
     * Generates widescreen scene artwork or square character portraits using gemini-nano-banana-2.1.
     */
    suspend fun generateArtwork(
        prompt: String,
        isPortrait: Boolean
    ): Result<Bitmap> {
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a visual prompt before generating artwork."))
        }
        return try {
            val styledPrompt = if (isPortrait) {
                "Cinematic painterly period film portrait, chiaroscuro golden rim light against deep midnight obsidian shadow, ultra-detailed editorial luxury cinema aesthetic, no text. Subject: $prompt"
            } else {
                "Widescreen prestige movie film still, dramatic chiaroscuro lighting, rich atmosphere, luxury cinematic color grading, ultra-detailed photography, no text or watermarks. Scene: $prompt"
            }
            val model = if (isPortrait) portraitImageModel else sceneImageModel
            val response = withTimeout(60_000L) {
                model.generateContent(styledPrompt)
            }
            val bitmap = response.candidates.firstOrNull()
                ?.content?.parts
                ?.filterIsInstance<ImagePart>()
                ?.firstOrNull()
                ?.image

            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("Model (${ModelIds.GEMINI_IMAGE}) did not return an image."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(Exception("Artwork generation timed out after 60 seconds."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Image generation quota exceeded", e)
            Result.failure(Exception("Image generation quota exceeded. Please check Firebase Blaze plan limits."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(Exception("Artwork generation failed: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    /**
     * Engine B: Google Gemini 3.8 Flash TTS (gemini-3.8-flash-tts on v1beta via Firebase AI Logic).
     * Passes speaker voice & acting style cleanly via structured speech_metadata in systemInstruction
     * (NEVER as inline spoken text), and converts returned PCM/WAV cleanly for Android playback.
     */
    suspend fun synthesizeGeminiTts(
        character: CharacterProfile,
        dialogueText: String,
        language: AppLanguage
    ): Result<SynthesizedAudioPayload> {
        if (dialogueText.isBlank()) {
            return Result.failure(IllegalArgumentException("Beat text is empty."))
        }
        return try {
            val actingStyle = character.displayActingStyle(language)
            val speechMetadata = JSONObject().apply {
                put("apiVersion", ModelIds.GEMINI_TTS_API_VERSION)
                put("model", ModelIds.GEMINI_TTS)
                put("speech_metadata", JSONObject().apply {
                    put("speaker_id", character.id)
                    put("speaker_name", character.displayName(language))
                    put("voice_name", character.geminiVoiceName)
                    put("acting_style", actingStyle)
                    put("language", if (language == AppLanguage.AR) "ar" else "en")
                    put("strict_delivery", "Speak ONLY the user dialogue verbatim. Never speak or read metadata, stage directions, or speaker names aloud.")
                })
            }

            val ttsModel = Firebase.ai.generativeModel(
                modelName = ModelIds.GEMINI_TTS,
                generationConfig = generationConfig {
                    responseModalities = listOf(ResponseModality.AUDIO)
                },
                systemInstruction = content("system") {
                    text(speechMetadata.toString())
                }
            )

            val response = withTimeout(60_000L) {
                ttsModel.generateContent(dialogueText.trim())
            }

            val audioPart = response.candidates.firstOrNull()
                ?.content?.parts
                ?.filterIsInstance<InlineDataPart>()
                ?.firstOrNull { it.mimeType.startsWith("audio/") || it.inlineData.isNotEmpty() }

            if (audioPart == null || audioPart.inlineData.isEmpty()) {
                return Result.failure(
                    IllegalStateException("Gemini TTS (${ModelIds.GEMINI_TTS}) returned no audio data.")
                )
            }

            val decoded = PcmWavConverter.ensurePlayableAudio(
                rawBytes = audioPart.inlineData,
                mimeType = audioPart.mimeType
            )
            Result.success(decoded)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini TTS timed out", e)
            Result.failure(Exception("Gemini 3.8 TTS timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini TTS failed", e)
            Result.failure(Exception("Gemini 3.8 TTS (${ModelIds.GEMINI_TTS}): ${e.localizedMessage ?: "Audio synthesis failed"}"))
        }
    }
}

/**
 * Decodes raw LPCM streams returned by Gemini TTS (e.g. audio/L16;codec=pcm;rate=24000)
 * into standards-compliant RIFF/WAVE files playable natively on Android.
 */
object PcmWavConverter {

    fun ensurePlayableAudio(rawBytes: ByteArray, mimeType: String): SynthesizedAudioPayload {
        val lowerMime = mimeType.lowercase()

        // Already a valid RIFF/WAVE container
        if (rawBytes.size >= 12 &&
            rawBytes[0] == 'R'.code.toByte() &&
            rawBytes[1] == 'I'.code.toByte() &&
            rawBytes[2] == 'F'.code.toByte() &&
            rawBytes[3] == 'F'.code.toByte()
        ) {
            return SynthesizedAudioPayload(rawBytes, "wav")
        }

        // MP3 container
        if (lowerMime.contains("mpeg") || lowerMime.contains("mp3")) {
            return SynthesizedAudioPayload(rawBytes, "mp3")
        }

        // OGG / Opus
        if (lowerMime.contains("ogg")) {
            return SynthesizedAudioPayload(rawBytes, "ogg")
        }

        // Raw PCM (audio/pcm or audio/L16;codec=pcm;rate=24000) -> wrap in 44-byte WAV header
        val rateRegex = Regex("rate=(\\d+)")
        val sampleRate = rateRegex.find(lowerMime)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 24000
        val channels = Regex("channels=(\\d+)").find(lowerMime)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1

        // If big-endian L16 is explicitly indicated without little-endian marker, swap pairs if needed.
        // Gemini TTS outputs little-endian 16-bit signed PCM by default.
        val wavBytes = wrapPcm16AsWav(rawBytes, sampleRate, channels, bitsPerSample = 16)
        return SynthesizedAudioPayload(wavBytes, "wav")
    }

    fun wrapPcm16AsWav(
        pcmData: ByteArray,
        sampleRate: Int = 24000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(chunkSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM subchunk size
            putShort(1) // AudioFormat = 1 (PCM)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign)
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        return ByteArrayOutputStream(44 + dataSize).apply {
            write(header)
            write(pcmData)
        }.toByteArray()
    }
}
