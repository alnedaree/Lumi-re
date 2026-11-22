package com.example.service

import com.example.model.CharacterProfile
import com.example.model.ElevenLabsModel
import com.example.model.ModelIds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ElevenLabsService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Synthesizes speech via official ElevenLabs v4 REST API.
     * STRICT MODEL LOCK: Only allows eleven_v4 (default) or eleven_v4_turbo.
     */
    suspend fun synthesizeSpeech(
        apiKey: String,
        model: ElevenLabsModel,
        character: CharacterProfile,
        text: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("ElevenLabs API key is missing. Open Voice Settings to enter your key.")
            )
        }
        val voiceId = character.elevenVoiceId.trim()
        if (voiceId.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Voice ID is empty for ${character.nameEn}. Set a Voice ID in Cast Manager.")
            )
        }

        // Strict verification against allowed flagship model IDs
        val enforcedModelId = when (model) {
            ElevenLabsModel.ELEVEN_V4 -> ModelIds.ELEVENLABS_DEFAULT
            ElevenLabsModel.ELEVEN_V4_TURBO -> ModelIds.ELEVENLABS_TURBO
        }

        val payload = JSONObject().apply {
            put("text", text)
            put("model_id", enforcedModelId)
            put("voice_settings", JSONObject().apply {
                put("stability", character.elevenStability.toDouble())
                put("similarity_boost", character.elevenSimilarityBoost.toDouble())
                put("style", character.elevenStyle.toDouble())
                put("use_speaker_boost", true)
            })
        }

        val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"
        val request = Request.Builder()
            .url(url)
            .addHeader("xi-api-key", cleanKey)
            .addHeader("Accept", "audio/mpeg")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        executeAudioRequest(request, "ElevenLabs ($enforcedModelId)")
    }

    /**
     * Generates cinematic sound effects via official ElevenLabs Sound Generation API
     * using eleven_text_to_sound_v3.
     */
    suspend fun generateSoundEffect(
        apiKey: String,
        prompt: String,
        durationSeconds: Double = 6.0,
        promptInfluence: Double = 0.45
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("ElevenLabs API key is required for SFX generation (${ModelIds.ELEVENLABS_SFX}).")
            )
        }
        if (prompt.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Enter an SFX description prompt first.")
            )
        }

        val payload = JSONObject().apply {
            put("text", prompt.trim())
            put("model_id", ModelIds.ELEVENLABS_SFX)
            put("duration_seconds", durationSeconds.coerceIn(1.0, 22.0))
            put("prompt_influence", promptInfluence.coerceIn(0.0, 1.0))
        }

        val request = Request.Builder()
            .url("https://api.elevenlabs.io/v1/sound-generation")
            .addHeader("xi-api-key", cleanKey)
            .addHeader("Accept", "audio/mpeg")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        executeAudioRequest(request, "ElevenLabs SFX (${ModelIds.ELEVENLABS_SFX})")
    }

    private fun executeAudioRequest(request: Request, label: String): Result<ByteArray> {
        return try {
            client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes()
                if (response.isSuccessful && bytes != null && bytes.isNotEmpty()) {
                    Result.success(bytes)
                } else {
                    val errorBody = bytes?.toString(Charsets.UTF_8).orEmpty()
                    val detail = extractElevenErrorDetail(errorBody)
                    val friendly = when (response.code) {
                        401 -> "$label: Unauthorized — verify your ElevenLabs API key."
                        404 -> "$label: Voice ID or endpoint not found. $detail"
                        422 -> "$label: Invalid request parameters. $detail"
                        429 -> "$label: Rate limit or character quota exceeded. $detail"
                        else -> "$label failed (HTTP ${response.code}). $detail"
                    }.trim()
                    Result.failure(IOException(friendly))
                }
            }
        } catch (e: Exception) {
            Result.failure(IOException("$label network error: ${e.localizedMessage ?: "Check internet connection"}", e))
        }
    }

    private fun extractElevenErrorDetail(body: String): String {
        if (body.isBlank()) return ""
        return try {
            val json = JSONObject(body)
            val detail = json.opt("detail")
            when (detail) {
                is JSONObject -> detail.optString("message", detail.optString("status", ""))
                is String -> detail
                else -> json.optString("message", "")
            }
        } catch (_: Exception) {
            body.take(140)
        }
    }
}
